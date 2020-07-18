#!/bin/bash

curPath=echo "$pwd"

echo "$curPath"

projects=$(ls -l  $curPat | awk '/^d/ { print $NF }')  #show all dir in cur path

function selectProject()
{
    echo "please select project you want to build."
    let i=0
    for dir in $(dir $curPat)
    do
        if [ -d $dir ]
        then
            let i+=1
            echo "$i $dir"
        fi
    done
}

function beginBuild()
{
    echo "$projects"
    projectName=$(echo "$projects" | sed -n "${1}p")  #show the inputnum dir, shou use "" instand of ''
#sed -n can only show the num line
    echo "$projectName"
    cd $projectName
    make clean
    make
}

echo "please select project you want to build."
selectProject
read -p "Please input the project index: " inputProject
beginBuild $inputProject

