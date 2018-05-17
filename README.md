# secure-copy
Copies files and creates a secure hash in the process. sha256sum compatible.

Audience interested in this tool is primarily video editors taking backups of many terabytes of data, mostly consisting of very large files.
File system is presumed user-friendly (no permission problems, no disk errors).

## Usage
* `SecureCopy --copy source_folder destination_folder` copies a source folder to the destination folder and creates **destination/sha256-(date).txt**. (date) is the seconds since epoch.
* `SecureCopy --verify folder`  locates **folder/sha256-(date).txt** and verifies if files in folder match SHA256 sums..

```
secure-copy.cmd --copy F:\media\2015 Z:\media\2015

java -cp target\classes;target\lib\commons-io-2.4.jar;target\classes;target\lib\commons-codec-1.10.jar org.securecopy.SecureCopy --copy F:\media\2015 Z:\media\2015
Index F:\media\2015...
Entries: 339 Dirs: 339 Files: 17335 Size: 2 TB
Benchmarking on F:\media\2015\2015-Q1\2015-03-19 OWASP TOR\bmpcc\bmpcc_1_2015-03-19_1844_C0000.mov:
       1024 2.4s
       4096 0.8s
      16384 0.4s
      65536 0.2s
     262144 0.2s
    1048576 0.5s
    4194304 0.5s
Selected blocksize: 262144
Queue depth: up to [943] of [262144] blocks (235 MB)
197 GB of 2 TB, 7,3% (180 MB/s)... Estimated time left: 3,9 hours
```

## Features
* SHA256 while copying, with different threads performing read, hashing, writing, i.e. virtually zero additional wall time clock increase getting sha256's.
* Attempts to estimate how much RAM is okay to use in the current VM. If source folder is read faster than destination folder, many reads are kept in RAM concurrently. 
  (ideally write disk is the bottle beck and there will always be ton) 
* Attempts to estimate optimal read block size yielding best read performance on by benchmarking on largest file in source folder.

## Considerations
* Very untested and over-engineered source code due to me attending a presentation of actor frameworks.
* Exception handling etc is so-so. If your disk is borked or you have messed with your java VM,  

## Building
Eclipse how-to.
* Install a JDK and set JDK as default JRE.
* Import project as Maven project.
* Right click project and select Run as -> Maven -> Maven Install. 

## Licence
Licensed under [MIT License](LICENSE), no strings attached.
Code is okay for re-use in open source projects, commercial projects, personal projects.
Be a nice person and tell me I rock if the code was helpful to you.
If you find a bug and produce a great fix, I'll merge smaller PR's that look good.
