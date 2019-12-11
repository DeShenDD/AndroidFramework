//从systemserver传入
public static PackageManagerService main(Context context, Installer installer,
        boolean factoryTest, boolean onlyCore) {
    // Self-check for initial settings.
    PackageManagerServiceCompilerMapping.checkProperties();

    //PMS构造函数
    PackageManagerService m = new PackageManagerService(context, installer,
            factoryTest, onlyCore);
    m.enableSystemUserPackages();
    //将PMS服务添加到servicemanager中，供其他应用或者服务通过binder调用
    ServiceManager.addService("package", m);
    //构造PMS Native层JNI接口
    final PackageManagerNative pmn = m.new PackageManagerNative();
    ServiceManager.addService("package_native", pmn);
    return m;  
}

    public PackageManagerService(Context context, Installer installer,
            boolean factoryTest, boolean onlyCore) {
        LockGuard.installLock(mPackages, LockGuard.INDEX_PACKAGES);
        Trace.traceBegin(TRACE_TAG_PACKAGE_MANAGER, "create package manager");
        EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_START,
                SystemClock.uptimeMillis());

        if (mSdkVersion <= 0) {
            Slog.w(TAG, "**** ro.build.version.sdk not set!");
        }

        mContext = context;

        mFactoryTest = factoryTest;
        mOnlyCore = onlyCore;
        mMetrics = new DisplayMetrics();
        mInstaller = installer;

        // Create sub-components that provide services / data. Order here is important.
        synchronized (mInstallLock) {
        synchronized (mPackages) {
            // Expose private service for system components to use.
            //构造所依赖的服务类对象
            LocalServices.addService(
                    PackageManagerInternal.class, new PackageManagerInternalImpl());
            sUserManager = new UserManagerService(context, this,
                    new UserDataPreparer(mInstaller, mInstallLock, mContext, mOnlyCore), mPackages);
            mComponentResolver = new ComponentResolver(sUserManager,
                    LocalServices.getService(PackageManagerInternal.class),
                    mPackages);
            mPermissionManager = PermissionManagerService.create(context,
                    mPackages /*externalLock*/);
            mDefaultPermissionPolicy = mPermissionManager.getDefaultPermissionGrantPolicy();
            //创建Setting，用来存储package的设置
            mSettings = new Settings(Environment.getDataDirectory(),
                    mPermissionManager.getPermissionSettings(), mPackages);
        }
        }
        //添加各种共享UID到setting中，使应用可以使用以下对应的共享库
        mSettings.addSharedUserLPw("android.uid.system", Process.SYSTEM_UID,
                ApplicationInfo.FLAG_SYSTEM, ApplicationInfo.PRIVATE_FLAG_PRIVILEGED);
        mSettings.addSharedUserLPw("android.uid.phone", RADIO_UID,
                ApplicationInfo.FLAG_SYSTEM, ApplicationInfo.PRIVATE_FLAG_PRIVILEGED);
        mSettings.addSharedUserLPw("android.uid.log", LOG_UID,
                ApplicationInfo.FLAG_SYSTEM, ApplicationInfo.PRIVATE_FLAG_PRIVILEGED);
        mSettings.addSharedUserLPw("android.uid.nfc", NFC_UID,
                ApplicationInfo.FLAG_SYSTEM, ApplicationInfo.PRIVATE_FLAG_PRIVILEGED);
        mSettings.addSharedUserLPw("android.uid.bluetooth", BLUETOOTH_UID,
                ApplicationInfo.FLAG_SYSTEM, ApplicationInfo.PRIVATE_FLAG_PRIVILEGED);
        mSettings.addSharedUserLPw("android.uid.shell", SHELL_UID,
                ApplicationInfo.FLAG_SYSTEM, ApplicationInfo.PRIVATE_FLAG_PRIVILEGED);
        mSettings.addSharedUserLPw("android.uid.se", SE_UID,
                ApplicationInfo.FLAG_SYSTEM, ApplicationInfo.PRIVATE_FLAG_PRIVILEGED);
        mSettings.addSharedUserLPw("android.uid.networkstack", NETWORKSTACK_UID,
                ApplicationInfo.FLAG_SYSTEM, ApplicationInfo.PRIVATE_FLAG_PRIVILEGED);

        String separateProcesses = SystemProperties.get("debug.separate_processes");
        if (separateProcesses != null && separateProcesses.length() > 0) {
            if ("*".equals(separateProcesses)) {
                mDefParseFlags = PackageParser.PARSE_IGNORE_PROCESSES;
                mSeparateProcesses = null;
                Slog.w(TAG, "Running with debug.separate_processes: * (ALL)");
            } else {
                mDefParseFlags = 0;
                mSeparateProcesses = separateProcesses.split(",");
                Slog.w(TAG, "Running with debug.separate_processes: "
                        + separateProcesses);
            }
        } else {
            mDefParseFlags = 0;
            mSeparateProcesses = null;
        }

        mPackageDexOptimizer = new PackageDexOptimizer(installer, mInstallLock, context,
                "*dexopt*");
        mDexManager = new DexManager(mContext, this, mPackageDexOptimizer, installer, mInstallLock);
        mArtManagerService = new ArtManagerService(mContext, this, installer, mInstallLock);
        mMoveCallbacks = new MoveCallbacks(FgThread.get().getLooper());

        mViewCompiler = new ViewCompiler(mInstallLock, mInstaller);

        mOnPermissionChangeListeners = new OnPermissionChangeListeners(
                FgThread.get().getLooper());

        getDefaultDisplayMetrics(context, mMetrics);

        //读取系统配置
        Trace.traceBegin(TRACE_TAG_PACKAGE_MANAGER, "get system config");
        SystemConfig systemConfig = SystemConfig.getInstance();
        mAvailableFeatures = systemConfig.getAvailableFeatures();
        Trace.traceEnd(TRACE_TAG_PACKAGE_MANAGER);

        mProtectedPackages = new ProtectedPackages(mContext);

        mApexManager = new ApexManager(context);
        synchronized (mInstallLock) {
        // writer
        synchronized (mPackages) {
            mHandlerThread = new ServiceThread(TAG,
                    Process.THREAD_PRIORITY_BACKGROUND, true /*allowIo*/);
            mHandlerThread.start();
            mHandler = new PackageHandler(mHandlerThread.getLooper());
            mProcessLoggingHandler = new ProcessLoggingHandler();
            Watchdog.getInstance().addThread(mHandler, WATCHDOG_TIMEOUT); //WatchDog用来监测异常
            mInstantAppRegistry = new InstantAppRegistry(this);

            ArrayMap<String, SystemConfig.SharedLibraryEntry> libConfig
                    = systemConfig.getSharedLibraries();
            final int builtInLibCount = libConfig.size();
            for (int i = 0; i < builtInLibCount; i++) {
                String name = libConfig.keyAt(i);
                SystemConfig.SharedLibraryEntry entry = libConfig.valueAt(i);
                addBuiltInSharedLibraryLocked(entry.filename, name);
            }

            // Now that we have added all the libraries, iterate again to add dependency
            // information IFF their dependencies are added.
            long undefinedVersion = SharedLibraryInfo.VERSION_UNDEFINED;
            for (int i = 0; i < builtInLibCount; i++) {
                String name = libConfig.keyAt(i);
                SystemConfig.SharedLibraryEntry entry = libConfig.valueAt(i);
                final int dependencyCount = entry.dependencies.length;
                for (int j = 0; j < dependencyCount; j++) {
                    final SharedLibraryInfo dependency =
                        getSharedLibraryInfoLPr(entry.dependencies[j], undefinedVersion);
                    if (dependency != null) {
                        getSharedLibraryInfoLPr(name, undefinedVersion).addDependency(dependency);
                    }
                }
            }

            SELinuxMMAC.readInstallPolicy();

            Trace.traceBegin(TRACE_TAG_PACKAGE_MANAGER, "loadFallbacks");
            FallbackCategoryProvider.loadFallbacks();
            Trace.traceEnd(TRACE_TAG_PACKAGE_MANAGER);

            Trace.traceBegin(TRACE_TAG_PACKAGE_MANAGER, "read user settings");
            mFirstBoot = !mSettings.readLPw(sUserManager.getUsers(false));//从package.xml读取存储的应用信息，以此来判断是否是首次开机
            Trace.traceEnd(TRACE_TAG_PACKAGE_MANAGER);

            // Clean up orphaned packages for which the code path doesn't exist
            // and they are an update to a system app - caused by bug/32321269
            //清理非正常卸载的应用对应的数据
            final int packageSettingCount = mSettings.mPackages.size();
            for (int i = packageSettingCount - 1; i >= 0; i--) {
                PackageSetting ps = mSettings.mPackages.valueAt(i);
                if (!isExternal(ps) && (ps.codePath == null || !ps.codePath.exists())
                        && mSettings.getDisabledSystemPkgLPr(ps.name) != null) {
                    mSettings.mPackages.removeAt(i);
                    mSettings.enableSystemPackageLPw(ps.name);//确保没有在disable名单中，也就是预置的有过升级的系统应用
                }
            }

            if (!mOnlyCore && mFirstBoot) {
                requestCopyPreoptedFiles();
            }

            String customResolverActivityName = Resources.getSystem().getString(
                    R.string.config_customResolverActivity);//读取ResolverActivity的系统资源
            if (!TextUtils.isEmpty(customResolverActivityName)) {
                mCustomResolverComponentName = ComponentName.unflattenFromString(
                        customResolverActivityName);
            }

            long startTime = SystemClock.uptimeMillis();

            EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_SYSTEM_SCAN_START,
                    startTime);

            //获取系统的环境变量
            final String bootClassPath = System.getenv("BOOTCLASSPATH");
            final String systemServerClassPath = System.getenv("SYSTEMSERVERCLASSPATH");

            if (bootClassPath == null) {
                Slog.w(TAG, "No BOOTCLASSPATH found!");
            }

            if (systemServerClassPath == null) {
                Slog.w(TAG, "No SYSTEMSERVERCLASSPATH found!");
            }

            File frameworkDir = new File(Environment.getRootDirectory(), "framework");

            final VersionInfo ver = mSettings.getInternalVersion();
            mIsUpgrade = !Build.FINGERPRINT.equals(ver.fingerprint);//根据fingerprint判断系统是否有OTA升级
            if (mIsUpgrade) {
                logCriticalInfo(Log.INFO,
                        "Upgrading from " + ver.fingerprint + " to " + Build.FINGERPRINT);
            }

            // when upgrading from pre-M, promote system app permissions from install to runtime
            mPromoteSystemApps =
                    mIsUpgrade && ver.sdkVersion <= Build.VERSION_CODES.LOLLIPOP_MR1;

            // When upgrading from pre-N, we need to handle package extraction like first boot,
            // as there is no profiling data available.
            mIsPreNUpgrade = mIsUpgrade && ver.sdkVersion < Build.VERSION_CODES.N;

            mIsPreNMR1Upgrade = mIsUpgrade && ver.sdkVersion < Build.VERSION_CODES.N_MR1;
            mIsPreQUpgrade = mIsUpgrade && ver.sdkVersion < Build.VERSION_CODES.Q;

            int preUpgradeSdkVersion = ver.sdkVersion;

            // save off the names of pre-existing system packages prior to scanning; we don't
            // want to automatically grant runtime permissions for new system apps
            //M之前的版本要对预置应用特殊处理
            if (mPromoteSystemApps) {
                Iterator<PackageSetting> pkgSettingIter = mSettings.mPackages.values().iterator();
                while (pkgSettingIter.hasNext()) {
                    PackageSetting ps = pkgSettingIter.next();
                    if (isSystemApp(ps)) {
                        mExistingSystemPackages.add(ps.name);
                    }
                }
            }

            mCacheDir = preparePackageParserCache();

            // Set flag to monitor and not change apk file paths when
            // scanning install directories.
            int scanFlags = SCAN_BOOTING | SCAN_INITIAL;

            if (mIsUpgrade || mFirstBoot) {
                scanFlags = scanFlags | SCAN_FIRST_BOOT_OR_UPGRADE;
            }

            // Collect vendor/product/product_services overlay packages. (Do this before scanning
            // any apps.)
            // For security and version matching reason, only consider overlay packages if they
            // reside in the right directory.
            //扫描安装系统预置应用
            scanDirTracedLI(new File(VENDOR_OVERLAY_DIR),
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_VENDOR,
                    0);
            scanDirTracedLI(new File(PRODUCT_OVERLAY_DIR),
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_PRODUCT,
                    0);
            scanDirTracedLI(new File(PRODUCT_SERVICES_OVERLAY_DIR),
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_PRODUCT_SERVICES,
                    0);
            scanDirTracedLI(new File(ODM_OVERLAY_DIR),
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_ODM,
                    0);
            scanDirTracedLI(new File(OEM_OVERLAY_DIR),
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_OEM,
                    0);

            mParallelPackageParserCallback.findStaticOverlayPackages();

            // Find base frameworks (resource packages without code).
            //扫描安装android应用包，没有安装成功需要抛出异常
            scanDirTracedLI(frameworkDir,
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_NO_DEX
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_PRIVILEGED,
                    0);
            if (!mPackages.containsKey("android")) {
                throw new IllegalStateException(
                        "Failed to load frameworks package; check log for warnings");
            }

            // Collect privileged system packages.
            final File privilegedAppDir = new File(Environment.getRootDirectory(), "priv-app");
            scanDirTracedLI(privilegedAppDir,
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_PRIVILEGED,
                    0);

            // Collect ordinary system packages.
            final File systemAppDir = new File(Environment.getRootDirectory(), "app");
            scanDirTracedLI(systemAppDir,
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM,
                    0);

            // Collect privileged vendor packages.
            File privilegedVendorAppDir = new File(Environment.getVendorDirectory(), "priv-app");
            try {
                privilegedVendorAppDir = privilegedVendorAppDir.getCanonicalFile();
            } catch (IOException e) {
                // failed to look up canonical path, continue with original one
            }
            scanDirTracedLI(privilegedVendorAppDir,
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_VENDOR
                    | SCAN_AS_PRIVILEGED,
                    0);

            // Collect ordinary vendor packages.
            File vendorAppDir = new File(Environment.getVendorDirectory(), "app");
            try {
                vendorAppDir = vendorAppDir.getCanonicalFile();
            } catch (IOException e) {
                // failed to look up canonical path, continue with original one
            }
            scanDirTracedLI(vendorAppDir,
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_VENDOR,
                    0);

            // Collect privileged odm packages. /odm is another vendor partition
            // other than /vendor.
            File privilegedOdmAppDir = new File(Environment.getOdmDirectory(),
                        "priv-app");
            try {
                privilegedOdmAppDir = privilegedOdmAppDir.getCanonicalFile();
            } catch (IOException e) {
                // failed to look up canonical path, continue with original one
            }
            scanDirTracedLI(privilegedOdmAppDir,
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_VENDOR
                    | SCAN_AS_PRIVILEGED,
                    0);

            // Collect ordinary odm packages. /odm is another vendor partition
            // other than /vendor.
            File odmAppDir = new File(Environment.getOdmDirectory(), "app");
            try {
                odmAppDir = odmAppDir.getCanonicalFile();
            } catch (IOException e) {
                // failed to look up canonical path, continue with original one
            }
            scanDirTracedLI(odmAppDir,
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_VENDOR,
                    0);

            // Collect all OEM packages.
            final File oemAppDir = new File(Environment.getOemDirectory(), "app");
            scanDirTracedLI(oemAppDir,
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_OEM,
                    0);

            // Collected privileged /product packages.
            File privilegedProductAppDir = new File(Environment.getProductDirectory(), "priv-app");
            try {
                privilegedProductAppDir = privilegedProductAppDir.getCanonicalFile();
            } catch (IOException e) {
                // failed to look up canonical path, continue with original one
            }
            scanDirTracedLI(privilegedProductAppDir,
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_PRODUCT
                    | SCAN_AS_PRIVILEGED,
                    0);

            // Collect ordinary /product packages.
            File productAppDir = new File(Environment.getProductDirectory(), "app");
            try {
                productAppDir = productAppDir.getCanonicalFile();
            } catch (IOException e) {
                // failed to look up canonical path, continue with original one
            }
            scanDirTracedLI(productAppDir,
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_PRODUCT,
                    0);

            // Collected privileged /product_services packages.
            File privilegedProductServicesAppDir =
                    new File(Environment.getProductServicesDirectory(), "priv-app");
            try {
                privilegedProductServicesAppDir =
                        privilegedProductServicesAppDir.getCanonicalFile();
            } catch (IOException e) {
                // failed to look up canonical path, continue with original one
            }
            scanDirTracedLI(privilegedProductServicesAppDir,
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_PRODUCT_SERVICES
                    | SCAN_AS_PRIVILEGED,
                    0);

            // Collect ordinary /product_services packages.
            File productServicesAppDir = new File(Environment.getProductServicesDirectory(), "app");
            try {
                productServicesAppDir = productServicesAppDir.getCanonicalFile();
            } catch (IOException e) {
                // failed to look up canonical path, continue with original one
            }
            scanDirTracedLI(productServicesAppDir,
                    mDefParseFlags
                    | PackageParser.PARSE_IS_SYSTEM_DIR,
                    scanFlags
                    | SCAN_AS_SYSTEM
                    | SCAN_AS_PRODUCT_SERVICES,
                    0);

            // Prune any system packages that no longer exist.
            final List<String> possiblyDeletedUpdatedSystemApps = new ArrayList<>();
            // Stub packages must either be replaced with full versions in the /data
            // partition or be disabled.
            final List<String> stubSystemApps = new ArrayList<>();
            if (!mOnlyCore) {
                // do this first before mucking with mPackages for the "expecting better" case
                //收集.stub应用，即需要全包替换的系统应用，一般不存在
                final Iterator<PackageParser.Package> pkgIterator = mPackages.values().iterator();
                while (pkgIterator.hasNext()) {
                    final PackageParser.Package pkg = pkgIterator.next();
                    if (pkg.isStub) {
                        stubSystemApps.add(pkg.packageName);
                    }
                }

                //从setting中取出已经安装的应用，此Setting中与Package中不同，因为还没有writeSetting
                final Iterator<PackageSetting> psit = mSettings.mPackages.values().iterator();
                while (psit.hasNext()) {
                    PackageSetting ps = psit.next();

                    /*
                     * If this is not a system app, it can't be a
                     * disable system app.
                     */
                    //去除非系统应用
                    if ((ps.pkgFlags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                        continue;
                    }

                    /*
                     * If the package is scanned, it's not erased.
                     */
                    final PackageParser.Package scannedPkg = mPackages.get(ps.name);
                    if (scannedPkg != null) {
                        /*
                         * If the system app is both scanned and in the
                         * disabled packages list, then it must have been
                         * added via OTA. Remove it from the currently
                         * scanned package so the previously user-installed
                         * application can be scanned
                         * 既被作为apk文件扫描到，同时原本是升级过的系统应用，
                         * 可以确认是OTA更新了预置系统应用的版本，需要特殊处理
                         */
                        if (mSettings.isDisabledSystemPackageLPr(ps.name)) {
                            logCriticalInfo(Log.WARN,
                                    "Expecting better updated system app for " + ps.name
                                    + "; removing system app.  Last known"
                                    + " codePath=" + ps.codePathString
                                    + ", versionCode=" + ps.versionCode
                                    + "; scanned versionCode=" + scannedPkg.getLongVersionCode());
                            removePackageLI(scannedPkg, true);
                            mExpectingBetter.put(ps.name, ps.codePath);
                        }

                        continue;
                    }

                    //不存在对应的setting，并且也不在disable列表中
                    if (!mSettings.isDisabledSystemPackageLPr(ps.name)) {
                        psit.remove();
                        logCriticalInfo(Log.WARN, "System package " + ps.name
                                + " no longer exists; it's data will be wiped");
                        // Actual deletion of code and data will be handled by later
                        // reconciliation step
                    } else {//删除已经不存在的应用
                        // we still have a disabled system package, but, it still might have
                        // been removed. check the code path still exists and check there's
                        // still a package. the latter can happen if an OTA keeps the same
                        // code path, but, changes the package name.
                        final PackageSetting disabledPs =
                                mSettings.getDisabledSystemPkgLPr(ps.name);
                        if (disabledPs.codePath == null || !disabledPs.codePath.exists()
                                || disabledPs.pkg == null) {
                            possiblyDeletedUpdatedSystemApps.add(ps.name);
                        } else {
                            // We're expecting that the system app should remain disabled, but add
                            // it to expecting better to recover in case the data version cannot
                            // be scanned.
                            mExpectingBetter.put(disabledPs.name, disabledPs.codePath);
                        }
                    }
                }
            }

            //delete tmp files
            deleteTempPackageFiles();

            final int cachedSystemApps = PackageParser.sCachedPackageReadCount.get();

            // Remove any shared userIDs that have no associated packages
            mSettings.pruneSharedUsersLPw();
            final long systemScanTime = SystemClock.uptimeMillis() - startTime;
            final int systemPackagesCount = mPackages.size();
            Slog.i(TAG, "Finished scanning system apps. Time: " + systemScanTime
                    + " ms, packageCount: " + systemPackagesCount
                    + " , timePerPackage: "
                    + (systemPackagesCount == 0 ? 0 : systemScanTime / systemPackagesCount)
                    + " , cached: " + cachedSystemApps);
            if (mIsUpgrade && systemPackagesCount > 0) {
                MetricsLogger.histogram(null, "ota_package_manager_system_app_avg_scan_time",
                        ((int) systemScanTime) / systemPackagesCount);
            }
            if (!mOnlyCore) {
                EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_DATA_SCAN_START,
                        SystemClock.uptimeMillis());
                scanDirTracedLI(sAppInstallDir, 0, scanFlags | SCAN_REQUIRE_KNOWN, 0);

                // Remove disable package settings for updated system apps that were
                // removed via an OTA. If the update is no longer present, remove the
                // app completely. Otherwise, revoke their system privileges.
                for (int i = possiblyDeletedUpdatedSystemApps.size() - 1; i >= 0; --i) {
                    final String packageName = possiblyDeletedUpdatedSystemApps.get(i);
                    final PackageParser.Package pkg = mPackages.get(packageName);
                    final String msg;

                    // remove from the disabled system list; do this first so any future
                    // scans of this package are performed without this state
                    mSettings.removeDisabledSystemPackageLPw(packageName);

                    if (pkg == null) {
                        // should have found an update, but, we didn't; remove everything
                        msg = "Updated system package " + packageName
                                + " no longer exists; removing its data";
                        // Actual deletion of code and data will be handled by later
                        // reconciliation step
                    } else {
                        // found an update; revoke system privileges
                        msg = "Updated system package " + packageName
                                + " no longer exists; rescanning package on data";

                        // NOTE: We don't do anything special if a stub is removed from the
                        // system image. But, if we were [like removing the uncompressed
                        // version from the /data partition], this is where it'd be done.

                        // remove the package from the system and re-scan it without any
                        // special privileges
                        removePackageLI(pkg, true);
                        try {
                            final File codePath = new File(pkg.applicationInfo.getCodePath());
                            scanPackageTracedLI(codePath, 0, scanFlags, 0, null);
                        } catch (PackageManagerException e) {
                            Slog.e(TAG, "Failed to parse updated, ex-system package: "
                                    + e.getMessage());
                        }
                    }

                    // one final check. if we still have a package setting [ie. it was
                    // previously scanned and known to the system], but, we don't have
                    // a package [ie. there was an error scanning it from the /data
                    // partition], completely remove the package data.
                    final PackageSetting ps = mSettings.mPackages.get(packageName);
                    if (ps != null && mPackages.get(packageName) == null) {
                        removePackageDataLIF(ps, null, null, 0, false);

                    }
                    logCriticalInfo(Log.WARN, msg);
                }

                /*
                 * Make sure all system apps that we expected to appear on
                 * the userdata partition actually showed up. If they never
                 * appeared, crawl back and revive the system version.
                 */
                for (int i = 0; i < mExpectingBetter.size(); i++) {
                    final String packageName = mExpectingBetter.keyAt(i);
                    if (!mPackages.containsKey(packageName)) {
                        final File scanFile = mExpectingBetter.valueAt(i);

                        logCriticalInfo(Log.WARN, "Expected better " + packageName
                                + " but never showed up; reverting to system");

                        final @ParseFlags int reparseFlags;
                        final @ScanFlags int rescanFlags;
                        if (FileUtils.contains(privilegedAppDir, scanFile)) {
                            reparseFlags =
                                    mDefParseFlags |
                                    PackageParser.PARSE_IS_SYSTEM_DIR;
                            rescanFlags =
                                    scanFlags
                                    | SCAN_AS_SYSTEM
                                    | SCAN_AS_PRIVILEGED;
                        } else if (FileUtils.contains(systemAppDir, scanFile)) {
                            reparseFlags =
                                    mDefParseFlags |
                                    PackageParser.PARSE_IS_SYSTEM_DIR;
                            rescanFlags =
                                    scanFlags
                                    | SCAN_AS_SYSTEM;
                        } else if (FileUtils.contains(privilegedVendorAppDir, scanFile)
                                || FileUtils.contains(privilegedOdmAppDir, scanFile)) {
                            reparseFlags =
                                    mDefParseFlags |
                                    PackageParser.PARSE_IS_SYSTEM_DIR;
                            rescanFlags =
                                    scanFlags
                                    | SCAN_AS_SYSTEM
                                    | SCAN_AS_VENDOR
                                    | SCAN_AS_PRIVILEGED;
                        } else if (FileUtils.contains(vendorAppDir, scanFile)
                                || FileUtils.contains(odmAppDir, scanFile)) {
                            reparseFlags =
                                    mDefParseFlags |
                                    PackageParser.PARSE_IS_SYSTEM_DIR;
                            rescanFlags =
                                    scanFlags
                                    | SCAN_AS_SYSTEM
                                    | SCAN_AS_VENDOR;
                        } else if (FileUtils.contains(oemAppDir, scanFile)) {
                            reparseFlags =
                                    mDefParseFlags |
                                    PackageParser.PARSE_IS_SYSTEM_DIR;
                            rescanFlags =
                                    scanFlags
                                    | SCAN_AS_SYSTEM
                                    | SCAN_AS_OEM;
                        } else if (FileUtils.contains(privilegedProductAppDir, scanFile)) {
                            reparseFlags =
                                    mDefParseFlags |
                                    PackageParser.PARSE_IS_SYSTEM_DIR;
                            rescanFlags =
                                    scanFlags
                                    | SCAN_AS_SYSTEM
                                    | SCAN_AS_PRODUCT
                                    | SCAN_AS_PRIVILEGED;
                        } else if (FileUtils.contains(productAppDir, scanFile)) {
                            reparseFlags =
                                    mDefParseFlags |
                                    PackageParser.PARSE_IS_SYSTEM_DIR;
                            rescanFlags =
                                    scanFlags
                                    | SCAN_AS_SYSTEM
                                    | SCAN_AS_PRODUCT;
                        } else if (FileUtils.contains(privilegedProductServicesAppDir, scanFile)) {
                            reparseFlags =
                                    mDefParseFlags |
                                    PackageParser.PARSE_IS_SYSTEM_DIR;
                            rescanFlags =
                                    scanFlags
                                    | SCAN_AS_SYSTEM
                                    | SCAN_AS_PRODUCT_SERVICES
                                    | SCAN_AS_PRIVILEGED;
                        } else if (FileUtils.contains(productServicesAppDir, scanFile)) {
                            reparseFlags =
                                    mDefParseFlags |
                                    PackageParser.PARSE_IS_SYSTEM_DIR;
                            rescanFlags =
                                    scanFlags
                                    | SCAN_AS_SYSTEM
                                    | SCAN_AS_PRODUCT_SERVICES;
                        } else {
                            Slog.e(TAG, "Ignoring unexpected fallback path " + scanFile);
                            continue;
                        }

                        mSettings.enableSystemPackageLPw(packageName);

                        try {
                            scanPackageTracedLI(scanFile, reparseFlags, rescanFlags, 0, null);
                        } catch (PackageManagerException e) {
                            Slog.e(TAG, "Failed to parse original system package: "
                                    + e.getMessage());
                        }
                    }
                }

                // Uncompress and install any stubbed system applications.
                // This must be done last to ensure all stubs are replaced or disabled.
                installSystemStubPackages(stubSystemApps, scanFlags);

                final int cachedNonSystemApps = PackageParser.sCachedPackageReadCount.get()
                                - cachedSystemApps;

                final long dataScanTime = SystemClock.uptimeMillis() - systemScanTime - startTime;
                final int dataPackagesCount = mPackages.size() - systemPackagesCount;
                Slog.i(TAG, "Finished scanning non-system apps. Time: " + dataScanTime
                        + " ms, packageCount: " + dataPackagesCount
                        + " , timePerPackage: "
                        + (dataPackagesCount == 0 ? 0 : dataScanTime / dataPackagesCount)
                        + " , cached: " + cachedNonSystemApps);
                if (mIsUpgrade && dataPackagesCount > 0) {
                    MetricsLogger.histogram(null, "ota_package_manager_data_app_avg_scan_time",
                            ((int) dataScanTime) / dataPackagesCount);
                }
            }
            mExpectingBetter.clear();

            // Resolve the storage manager.
            mStorageManagerPackage = getStorageManagerPackageName();

            // Resolve protected action filters. Only the setup wizard is allowed to
            // have a high priority filter for these actions.
            mSetupWizardPackage = getSetupWizardPackageName();
            mComponentResolver.fixProtectedFilterPriorities();

            mSystemTextClassifierPackage = getSystemTextClassifierPackageName();

            mWellbeingPackage = getWellbeingPackageName();
            mDocumenterPackage = getDocumenterPackageName();
            mConfiguratorPackage =
                    mContext.getString(R.string.config_deviceConfiguratorPackageName);
            mAppPredictionServicePackage = getAppPredictionServicePackageName();
            mIncidentReportApproverPackage = getIncidentReportApproverPackageName();

            // Now that we know all of the shared libraries, update all clients to have
            // the correct library paths.
            updateAllSharedLibrariesLocked(null, Collections.unmodifiableMap(mPackages));

            for (SharedUserSetting setting : mSettings.getAllSharedUsersLPw()) {
                // NOTE: We ignore potential failures here during a system scan (like
                // the rest of the commands above) because there's precious little we
                // can do about it. A settings error is reported, though.
                final List<String> changedAbiCodePath =
                        adjustCpuAbisForSharedUserLPw(setting.packages, null /*scannedPackage*/);
                if (changedAbiCodePath != null && changedAbiCodePath.size() > 0) {
                    for (int i = changedAbiCodePath.size() - 1; i >= 0; --i) {
                        final String codePathString = changedAbiCodePath.get(i);
                        try {
                            mInstaller.rmdex(codePathString,
                                    getDexCodeInstructionSet(getPreferredInstructionSet()));
                        } catch (InstallerException ignored) {
                        }
                    }
                }
                // Adjust seInfo to ensure apps which share a sharedUserId are placed in the same
                // SELinux domain.
                setting.fixSeInfoLocked();
            }

            // Now that we know all the packages we are keeping,
            // read and update their last usage times.
            mPackageUsage.read(mPackages);
            mCompilerStats.read();

            EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_SCAN_END,
                    SystemClock.uptimeMillis());
            Slog.i(TAG, "Time to scan packages: "
                    + ((SystemClock.uptimeMillis()-startTime)/1000f)
                    + " seconds");

            // If the platform SDK has changed since the last time we booted,
            // we need to re-grant app permission to catch any new ones that
            // appear.  This is really a hack, and means that apps can in some
            // cases get permissions that the user didn't initially explicitly
            // allow...  it would be nice to have some better way to handle
            // this situation.
            final boolean sdkUpdated = (ver.sdkVersion != mSdkVersion);
            if (sdkUpdated) {
                Slog.i(TAG, "Platform changed from " + ver.sdkVersion + " to "
                        + mSdkVersion + "; regranting permissions for internal storage");
            }
            mPermissionManager.updateAllPermissions(
                    StorageManager.UUID_PRIVATE_INTERNAL, sdkUpdated, mPackages.values(),
                    mPermissionCallback);
            ver.sdkVersion = mSdkVersion;

            // If this is the first boot or an update from pre-M, and it is a normal
            // boot, then we need to initialize the default preferred apps across
            // all defined users.
            if (!onlyCore && (mPromoteSystemApps || mFirstBoot)) {
                for (UserInfo user : sUserManager.getUsers(true)) {
                    mSettings.applyDefaultPreferredAppsLPw(user.id);
                    primeDomainVerificationsLPw(user.id);
                }
            }

            // Prepare storage for system user really early during boot,
            // since core system apps like SettingsProvider and SystemUI
            // can't wait for user to start
            final int storageFlags;
            if (StorageManager.isFileEncryptedNativeOrEmulated()) {
                storageFlags = StorageManager.FLAG_STORAGE_DE;
            } else {
                storageFlags = StorageManager.FLAG_STORAGE_DE | StorageManager.FLAG_STORAGE_CE;
            }
            List<String> deferPackages = reconcileAppsDataLI(StorageManager.UUID_PRIVATE_INTERNAL,
                    UserHandle.USER_SYSTEM, storageFlags, true /* migrateAppData */,
                    true /* onlyCoreApps */);
            mPrepareAppDataFuture = SystemServerInitThreadPool.get().submit(() -> {
                TimingsTraceLog traceLog = new TimingsTraceLog("SystemServerTimingAsync",
                        Trace.TRACE_TAG_PACKAGE_MANAGER);
                traceLog.traceBegin("AppDataFixup");
                try {
                    mInstaller.fixupAppData(StorageManager.UUID_PRIVATE_INTERNAL,
                            StorageManager.FLAG_STORAGE_DE | StorageManager.FLAG_STORAGE_CE);
                } catch (InstallerException e) {
                    Slog.w(TAG, "Trouble fixing GIDs", e);
                }
                traceLog.traceEnd();

                traceLog.traceBegin("AppDataPrepare");
                if (deferPackages == null || deferPackages.isEmpty()) {
                    return;
                }
                int count = 0;
                for (String pkgName : deferPackages) {
                    PackageParser.Package pkg = null;
                    synchronized (mPackages) {
                        PackageSetting ps = mSettings.getPackageLPr(pkgName);
                        if (ps != null && ps.getInstalled(UserHandle.USER_SYSTEM)) {
                            pkg = ps.pkg;
                        }
                    }
                    if (pkg != null) {
                        synchronized (mInstallLock) {
                            prepareAppDataAndMigrateLIF(pkg, UserHandle.USER_SYSTEM, storageFlags,
                                    true /* maybeMigrateAppData */);
                        }
                        count++;
                    }
                }
                traceLog.traceEnd();
                Slog.i(TAG, "Deferred reconcileAppsData finished " + count + " packages");
            }, "prepareAppData");

            // If this is first boot after an OTA, and a normal boot, then
            // we need to clear code cache directories.
            // Note that we do *not* clear the application profiles. These remain valid
            // across OTAs and are used to drive profile verification (post OTA) and
            // profile compilation (without waiting to collect a fresh set of profiles).
            if (mIsUpgrade && !onlyCore) {
                Slog.i(TAG, "Build fingerprint changed; clearing code caches");
                for (int i = 0; i < mSettings.mPackages.size(); i++) {
                    final PackageSetting ps = mSettings.mPackages.valueAt(i);
                    if (Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, ps.volumeUuid)) {
                        // No apps are running this early, so no need to freeze
                        clearAppDataLIF(ps.pkg, UserHandle.USER_ALL,
                                FLAG_STORAGE_DE | FLAG_STORAGE_CE | FLAG_STORAGE_EXTERNAL
                                        | Installer.FLAG_CLEAR_CODE_CACHE_ONLY);
                    }
                }
                ver.fingerprint = Build.FINGERPRINT;
            }

            // Grandfather existing (installed before Q) non-system apps to hide
            // their icons in launcher.
            if (!onlyCore && mIsPreQUpgrade) {
                Slog.i(TAG, "Whitelisting all existing apps to hide their icons");
                int size = mSettings.mPackages.size();
                for (int i = 0; i < size; i++) {
                    final PackageSetting ps = mSettings.mPackages.valueAt(i);
                    if ((ps.pkgFlags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        continue;
                    }
                    ps.disableComponentLPw(PackageManager.APP_DETAILS_ACTIVITY_CLASS_NAME,
                            UserHandle.USER_SYSTEM);
                }
            }

            // clear only after permissions and other defaults have been updated
            mExistingSystemPackages.clear();
            mPromoteSystemApps = false;

            // All the changes are done during package scanning.
            ver.databaseVersion = Settings.CURRENT_DATABASE_VERSION;

            // can downgrade to reader
            Trace.traceBegin(TRACE_TAG_PACKAGE_MANAGER, "write settings");
            mSettings.writeLPr();
            Trace.traceEnd(TRACE_TAG_PACKAGE_MANAGER);
            EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_PMS_READY,
                    SystemClock.uptimeMillis());

            if (!mOnlyCore) {
                mRequiredVerifierPackage = getRequiredButNotReallyRequiredVerifierLPr();
                mRequiredInstallerPackage = getRequiredInstallerLPr();
                mRequiredUninstallerPackage = getRequiredUninstallerLPr();
                mIntentFilterVerifierComponent = getIntentFilterVerifierComponentNameLPr();
                if (mIntentFilterVerifierComponent != null) {
                    mIntentFilterVerifier = new IntentVerifierProxy(mContext,
                            mIntentFilterVerifierComponent);
                } else {
                    mIntentFilterVerifier = null;
                }
                mServicesSystemSharedLibraryPackageName = getRequiredSharedLibraryLPr(
                        PackageManager.SYSTEM_SHARED_LIBRARY_SERVICES,
                        SharedLibraryInfo.VERSION_UNDEFINED);
                mSharedSystemSharedLibraryPackageName = getRequiredSharedLibraryLPr(
                        PackageManager.SYSTEM_SHARED_LIBRARY_SHARED,
                        SharedLibraryInfo.VERSION_UNDEFINED);
            } else {
                mRequiredVerifierPackage = null;
                mRequiredInstallerPackage = null;
                mRequiredUninstallerPackage = null;
                mIntentFilterVerifierComponent = null;
                mIntentFilterVerifier = null;
                mServicesSystemSharedLibraryPackageName = null;
                mSharedSystemSharedLibraryPackageName = null;
            }
            // PermissionController hosts default permission granting and role management, so it's a
            // critical part of the core system.
            mRequiredPermissionControllerPackage = getRequiredPermissionControllerLPr();

            // Initialize InstantAppRegistry's Instant App list for all users.
            final int[] userIds = UserManagerService.getInstance().getUserIds();
            for (PackageParser.Package pkg : mPackages.values()) {
                if (pkg.isSystem()) {
                    continue;
                }
                for (int userId : userIds) {
                    final PackageSetting ps = (PackageSetting) pkg.mExtras;
                    if (ps == null || !ps.getInstantApp(userId) || !ps.getInstalled(userId)) {
                        continue;
                    }
                    mInstantAppRegistry.addInstantAppLPw(userId, ps.appId);
                }
            }

            mInstallerService = new PackageInstallerService(context, this, mApexManager);
            final Pair<ComponentName, String> instantAppResolverComponent =
                    getInstantAppResolverLPr();
            if (instantAppResolverComponent != null) {
                if (DEBUG_INSTANT) {
                    Slog.d(TAG, "Set ephemeral resolver: " + instantAppResolverComponent);
                }
                mInstantAppResolverConnection = new InstantAppResolverConnection(
                        mContext, instantAppResolverComponent.first,
                        instantAppResolverComponent.second);
                mInstantAppResolverSettingsComponent =
                        getInstantAppResolverSettingsLPr(instantAppResolverComponent.first);
            } else {
                mInstantAppResolverConnection = null;
                mInstantAppResolverSettingsComponent = null;
            }
            updateInstantAppInstallerLocked(null);

            // Read and update the usage of dex files.
            // Do this at the end of PM init so that all the packages have their
            // data directory reconciled.
            // At this point we know the code paths of the packages, so we can validate
            // the disk file and build the internal cache.
            // The usage file is expected to be small so loading and verifying it
            // should take a fairly small time compare to the other activities (e.g. package
            // scanning).
            final Map<Integer, List<PackageInfo>> userPackages = new HashMap<>();
            for (int userId : userIds) {
                userPackages.put(userId, getInstalledPackages(/*flags*/ 0, userId).getList());
            }
            mDexManager.load(userPackages);
            if (mIsUpgrade) {
                MetricsLogger.histogram(null, "ota_package_manager_init_time",
                        (int) (SystemClock.uptimeMillis() - startTime));
            }
        } // synchronized (mPackages)
        } // synchronized (mInstallLock)

        mModuleInfoProvider = new ModuleInfoProvider(mContext, this);

        // Now after opening every single application zip, make sure they
        // are all flushed.  Not really needed, but keeps things nice and
        // tidy.
        Trace.traceBegin(TRACE_TAG_PACKAGE_MANAGER, "GC");
        Runtime.getRuntime().gc();
        Trace.traceEnd(TRACE_TAG_PACKAGE_MANAGER);

        // The initial scanning above does many calls into installd while
        // holding the mPackages lock, but we're mostly interested in yelling
        // once we have a booted system.
        mInstaller.setWarnIfHeld(mPackages);

        PackageParser.readConfigUseRoundIcon(mContext.getResources());

        mServiceStartWithDelay = SystemClock.uptimeMillis() + (60 * 1000L);

        Trace.traceEnd(TRACE_TAG_PACKAGE_MANAGER);
    }