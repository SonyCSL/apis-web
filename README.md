# apis-web

## Introduction
apis-webは電力融通の開発や運用保守のためのWebサービス等に対して電力融通に関わる情報を提供する  
ソフトウェアである。  
apis-webはコミュニケーションラインに接続された複数のapis-mainと共にクラスタを構築し、  
Grid Master(GM)やapis-mainからDC/DC ConverterやBattery等のハードウェア情報や
電力融通情報を取得する。    
Webサービスはそれらの情報をapis-webのWeb APIを利用することで取得することが可能である。  
また、Debug用に電力融通やErrorを生成する機能も有する。  

![apis-web](https://user-images.githubusercontent.com/71874910/94901565-c8e41980-04d1-11eb-9c38-c751a6acbdd9.PNG)

## Installation
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
$ cd ../
$ mkdir apis-web_exe
$ cp ./apis-web/target/apis-web-*-fat.jar ./apis-web_exe
$ cp ./apis-web/setting_files/* ./apis-web_exe
```

## Parameter Setting
Set the following file parameters in the apis-web_exe at least to suit your environment.   
Refer to "Chapter 7, About Configuration Files" in the [apis-web_specification](#anchor1) for more information.

&emsp;config.json   
&emsp;&emsp;&emsp;- communityId   &emsp;(default : oss_communityId)  
&emsp;&emsp;&emsp;- clusterId     &emsp;(default : oss_clusterId)  

&emsp;cluster.xml  
&emsp;&emsp;&emsp;- \<interface\>  &emsp;(default : 127.0.0.1)

&emsp;start.sh  
&emsp;&emsp;&emsp;- java arguments &emsp;(default : 127.0.0.1) 


## Running

```bash
$ cd apis-web_exe
$ bash start.sh
```

<a id="anchor1"></a>
## Documentation
&emsp;[apis-web_specification(JP)](https://github.com/SonyCSL/apis-web/blob/master/doc/jp/apis-web_specification.md)



## License
&emsp;[Apache License Version 2.0](https://github.com/SonyCSL/apis-web/blob/master/LICENSE)


## Notice
&emsp;[Notice](https://github.com/SonyCSL/apis-web/blob/master/NOTICE.md)
