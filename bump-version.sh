#!/bin/bash

. $(dirname $0)/functions/git-maven-bump

TAG=$1

git_maven_bump $TAG "."
