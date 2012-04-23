#!/bin/bash

. $(dirname $0)/functions/git-maven-bump

git_maven_bump $TAG "."
