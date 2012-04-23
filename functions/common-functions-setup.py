#!/usr/bin/python
#
# common-functions
#

import os
import sys
import subprocess

def os_exec(run):
  p = subprocess.Popen(run, shell=True, bufsize=1024, stdin=subprocess.PIPE,
    stdout=subprocess.PIPE, stderr=subprocess.PIPE, close_fds=True)

  body = p.stdout.read()
  
  code = p.wait()
  if code != 0:
    raise subprocess.CalledProcessError(code, run)

  return body

def git_show(branch, file):
  run = "git show " + branch + ":" + file;
  return os_exec(run);

def git_remote_add(origin, url):
  run = "git remote add " + origin + " " + url;
  return os_exec(run);

def git_add(file):
  run = "git add " + file;
  return os_exec(run);

def git_fetch(origin):
  run = "git fetch " + origin;
  return os_exec(run);

def git_status(path):
  run = "git status --porcelain -uno " + path;
  return os_exec(run);

def git_ls_tree(branch):
  run = "git ls-tree --full-tree --name-only -r " + branch;
  return os_exec(run);

def git_commit(comment, path):
  run = "git commit -m '" + comment + "' " + path;
  return os_exec(run);

def git_current_branch():
  run = "git branch --no-color 2> /dev/null | sed -e '/^[^*]/d' -e 's/* \(.*\)/\\1/'"
  return os_exec(run);

def git_remote_branches(filter):
  run = "git branch -r | grep '" + filter + "'"
  return os_exec(run);

def git_compare(bs, bc):
  run = "git diff --name-status " + bs + ".." + bc
  out = os_exec(run);
  out = out.split("\n")
  return out

def common_checkout(branch, func):
  body = git_show(branch, func)
  f = open(func, 'w')
  f.write(body)
  f.close()

def main(args):
  if len(sys.argv) == 2:
    func = args[1]
    common_checkout("functions-bin/master", func)
  else:
    git_remote_add("functions-bin", "https://github.com/axet/functions-bin.git")
    git_fetch("functions-bin")
    common_checkout("functions-bin/master", "common-functions-setup.py")
    common_checkout("functions-bin/master", "common-functions-merge.py")

if __name__ == "__main__":
  main(sys.argv)
