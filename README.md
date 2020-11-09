# "小游戏SDK” 对接文档


- 示例
   -  [MainActivity.java](https://github.com/YangChengTeam/androiduuuid/blob/master/app/src/main/java/com/yc/androidunicodeexample/MainActivity.java)

- 配置
  - 导入aar msa_mdid_1.0.13  [下载msa_mdid_1.0.13](https://github.com/YangChengTeam/androiduuuid/blob/master/lib/libs/msa_mdid_1.0.13.aar)
  - 根目录build.gradle maven配置  [查看](https://github.com/YangChengTeam/androiduuuid/blob/master/build.gradle)
    -           maven {url 'https://raw.githubusercontent.com/YangChengTeam/androiduuuid/master/lib/AAR'}
  - 项目build.gradle  implementation 'com.yc.uuid:aar:1.0.6'

  - AndroidManifest.xml 权限配置  [查看](https://github.com/YangChengTeam/androiduuuid/blob/master/lib/src/main/AndroidManifest.xml)

- 代码
   -  初始化
      -  UDID.getInstance(this).init();
   -  获取UDIDInfo
      -  UDID.getInstance(MainActivity.this).build();
  
