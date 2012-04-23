#!/usr/bin/python
#
# common-functions
#

import os

import imp
imp.load_source("m", os.path.join(os.path.dirname(os.path.abspath(__file__)), "common-functions-setup.py"))
from m import *

def common_ignore(branch):
  try:
    ignore = git_show(branch, ".common-ignore").split()
    ignore.append(".common-ignore")
    return ignore
  except subprocess.CalledProcessError:
    return []

def common_functions_merge(branch):
  changes = git_status(".")
  if changes:
    raise Exception("we cant operatate on modified tree")

  ignore = common_ignore(branch)
  
  # download only files exists in current directory
  list = git_ls_tree(branch).split()
  for l in list:
    if os.path.isfile(l) and not (l in ignore):
      common_checkout(branch, l)

  changes = git_status(".")
  if changes:
    git_commit("merge " + branch + " with last version", ".")

def main(args):
  os.chdir(os.path.dirname(os.path.abspath(__file__)))
  
  branch = "functions-bin/master"
  origin = branch.split("/")[0]
  git_fetch(origin)
  common_functions_merge(branch)

if __name__ == "__main__":
  main(sys.argv)
