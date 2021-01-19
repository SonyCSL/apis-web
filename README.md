# apis-web

## Introduction
apis-web is a software program that provides power interchange-related information to Web services (such as a visualization service) for the development, operation, and maintenance of power interchange. To get information from apis-main installed in multiple nodes connected on a communication lines such as Ethernet, apis-web forms a cluster by using Vert.x and Hazelcast framework functions. From Grid Master in the cluster, it gets hardware information such as the DC/DC converter and battery RSoC information of all nodes and power interchange information from any node. apis-web also has functions to generate power interchange and errors for debugging purposes.

Refer to the [apis-web_specification](#anchor1)  for more information.

![apis-web](https://user-images.githubusercontent.com/71874910/94901565-c8e41980-04d1-11eb-9c38-c751a6acbdd9.PNG)

## Installation
Here is how to install apis-web individually.  
git, maven, groovy and JDK must be installed in advance.

```bash
$ git clone https://github.com/SonyCSL/apis-bom.git
$ cd apis-bom
$ mvn install
$ cd ../
$ git clone https://github.com/SonyCSL/apis-common.git
$ cd apis-common
$ mvn install
$ cd ../
$ git cone https://github.com/SonyCSL/apis-web.git
$ cd apis-web
$ mvn package
```

## Running

Here is how to run apis-web individually.  

```bash
$ cd exe
$ bash start.sh
```

## Stopping

Here is how to stop apis-web individually.  

```bash
$ cd exe
$ bash stop.sh
```

## Parameter Setting
Set the following file parameters in the exe folder as necessary.   
Refer to "Chapter 7, About Configuration Files" in the [apis-web_specification](#anchor1) for more information.

&emsp;config.json   
&emsp;&emsp;&emsp;- communityId   &emsp;(default : oss_communityId)  
&emsp;&emsp;&emsp;- clusterId     &emsp;(default : oss_clusterId)  

&emsp;cluster.xml  
&emsp;&emsp;&emsp;- \<member\>  &emsp;(default : 127.0.0.1)  
&emsp;&emsp;&emsp;- \<interface\>  &emsp;(default : 127.0.0.1)  

&emsp;start.sh  
&emsp;&emsp;&emsp;-conf &emsp; (default : ./config.json)  
&emsp;&emsp;&emsp;-cluster-host &emsp; (default : 127.0.0.1)    


<a id="anchor1"></a>
## Documentation
&emsp;[apis-web_specification(EN)](https://github.com/SonyCSL/apis-web/blob/master/doc/en/apis-web_specification_en.md)  
&emsp;[apis-web_specification(JP)](https://github.com/SonyCSL/apis-web/blob/master/doc/jp/apis-web_specification.md)

## API Specification  

An example of creating an API specification using the Javadoc command is shown below.  
(For Ubuntu18.04)  
  
```bash  
$ export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/  
$ cd apis-web  
$ mvn javadoc:javadoc  
```  

The API specification is created in apis-main/target/site/apidocs/.  


## License
&emsp;[Apache License Version 2.0](https://github.com/SonyCSL/apis-web/blob/master/LICENSE)


## Notice
&emsp;[Notice](https://github.com/SonyCSL/apis-web/blob/master/NOTICE.md)
