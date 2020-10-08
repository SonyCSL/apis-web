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

## Getting Started
```bash

$ mkdir apis-common
$ cd apis-common
$ git clone https://github.com/SonyCSL/apis-common.git
$ mvn install
$ cd ..
$ mkdir apis-web
$ cd apis-web
$ git cone https://github.com/SonyCSL/apis-web.git
$ mvn package

```

## Usage


## Documentation
&emsp;[apis-web_specification(JP)](https://github.com/SonyCSL/apis-web/blob/master/doc/jp/apis-web_specification.md)



## License
&emsp;[Apache License Version 2.0](https://github.com/oes-github/apis-web/blob/master/LICENSE)


## Notice
&emsp;[Notice](https://github.com/oes-github/apis-web/blob/master/NOTICE.md)
