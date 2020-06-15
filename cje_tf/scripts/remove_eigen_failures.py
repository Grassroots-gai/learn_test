import argparse

def deleteKnownFailures(eigenFailure, utFailure, utOnlyFailure):
    o = open(utOnlyFailure, 'w')
    f1 = open(utFailure, 'r')
    for line1 in f1:
        find = False
        f2 = open(eigenFailure, 'r')
        for line2 in f2:
            if line1 in line2:
               find = True
               break
        f2.close()
        # did not find line1 in eigen failure so write to output file
        if( find == False ):
            o.write(line1)
    f1.close()
    o.close()

def main():
    # get and parse command line options
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseFailureFile", type=str, default="eigen.failures",
                    help="The base failure file, e.g. eigen.failures, or dnn.failures")
    parser.add_argument("--utFailureFile", type=str, default="ut.failures",
                    help="The new failure file, e.g. ut.failures")
    parser.add_argument("--newFailureFile", type=str, default="new.failures",
                    help="The failures file contains the failures only in utFailureFile and not in baseFailureFile, default is new.failures")
    args = parser.parse_args()

    deleteKnownFailures(args.baseFailureFile, args.utFailureFile, args.newFailureFile)

if __name__ == "__main__":
    main()
