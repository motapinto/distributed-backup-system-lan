#! /usr/bin/bash

# Placeholder for setup script
# To be executed on the root of the compiled tree
# Requires one argument: the peer id
# Sets up the directory tree for storing 
#  both the chunks and the restored files of
#  either a single peer, in which case you may or not use the argument
#    or for all peers, in which case you 


# Check number input arguments
argc=$#

if ((argc == 1 ))
then
	peer_id=$1
else
	echo "Usage: $0 [<peer_id>]]"
	exit 1
fi

# Build the directory tree for storing files
# For a crash course on shell commands check for example:
# Command line basi commands from GitLab Docs':	https://docs.gitlab.com/ee/gitlab-basics/command-line-commands.html
# For shell scripting try out the following tutorials of the Linux Documentation Project
# Bash Guide for Beginners: https://tldp.org/LDP/Bash-Beginners-Guide/html/index.html
# Advanced Bash Scripting: https://tldp.org/LDP/abs/html/

