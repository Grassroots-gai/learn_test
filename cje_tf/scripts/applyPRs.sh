#/bin/bash
#set -x
Usage="./applyPRs.sh -b branchname -r commit-id [PRs]"
repo=NervanaSystems
project=private-tensorflow
#repoaddrs="https://github.com/$repo/$project"
repoaddrs="git@github.com:$repo/$project"
patchrepo="https://github.com/tensorflow/tensorflow"
date=`date`

clean_up()
{
   rm -rf /tmp/patch.$$
}
error_exit()
{
   echo $*
   clean_up
   exit 1
}

comitid=""

if [ $# -lt 3 -a  "$1" != "-b" ]  
then
   error_exit $Usage
fi
branchname=$2
if [ $branchname = "master" ] 
then 
   error_exit "Branch cannot be master"
    #echo "WARNING: YOU ARE MODIFYING THE MASTER BRANCH!!!!"
fi
shift
shift
if [ $# -lt 3  -a "$1" != "-r" ] 
then
    error_exit $Usage
fi
comitid=$2
shift
shift

[ $# -eq 0 ] && error_exit $Usage
mkdir -p pr_update
cd pr_update
rm -rf $project
git clone $repoaddrs.git
cd $project
git checkout $branchname 
[ $? -ne 0 ] && error_exit "Cannot checkout branch"

git reset --hard $comitid
git rebase master

commit_string="Commit with PRs merged: "

while [[ $# -gt 0 ]]
do
	curl -o /tmp/patch.$$ -L $patchrepo/pull/$1.diff > /dev/null 2>&1
    [ $? -ne 0 ] && error_exit "No patch" $1 "not found"
	git apply /tmp/patch.$$ 
    [ $? -ne 0 ] && error_exit "Cannot apply patch " $1
	echo "Patch for $1 applied sucessfully"
	commit_string="$commit_string $1"
	shift
done

git add .
git commit -m "$commit_string"
git push -f 
clean_up
#rm -rf $pr_update
