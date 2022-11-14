@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  tms-utkast startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and TMS_UTKAST_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\tms-utkast.jar;%APP_HOME%\lib\dittnav-common-influxdb-2022.09.30-12.41-aa46d2d75788.jar;%APP_HOME%\lib\dittnav-common-utils-2022.09.30-12.41-aa46d2d75788.jar;%APP_HOME%\lib\rapids-and-rivers-2022100711511665136276.49acbaae4ed4.jar;%APP_HOME%\lib\ktor-server-cio-jvm-2.1.2.jar;%APP_HOME%\lib\ktor-server-metrics-micrometer-jvm-2.1.2.jar;%APP_HOME%\lib\ktor-server-host-common-jvm-2.1.2.jar;%APP_HOME%\lib\ktor-http-cio-jvm-2.1.2.jar;%APP_HOME%\lib\ktor-server-core-jvm-2.1.2.jar;%APP_HOME%\lib\ktor-serialization-jvm-2.1.2.jar;%APP_HOME%\lib\ktor-websockets-jvm-2.1.2.jar;%APP_HOME%\lib\ktor-network-jvm-2.1.2.jar;%APP_HOME%\lib\ktor-events-jvm-2.1.2.jar;%APP_HOME%\lib\ktor-http-jvm-2.1.2.jar;%APP_HOME%\lib\ktor-utils-jvm-2.1.2.jar;%APP_HOME%\lib\ktor-io-jvm-2.1.2.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.7.20.jar;%APP_HOME%\lib\flyway-core-6.5.7.jar;%APP_HOME%\lib\kotliquery-1.9.0.jar;%APP_HOME%\lib\HikariCP-4.0.3.jar;%APP_HOME%\lib\influxdb-java-2.20.jar;%APP_HOME%\lib\postgresql-42.4.1.jar;%APP_HOME%\lib\micrometer-registry-prometheus-1.9.4.jar;%APP_HOME%\lib\simpleclient_common-0.15.0.jar;%APP_HOME%\lib\simpleclient_hotspot-0.9.0.jar;%APP_HOME%\lib\simpleclient_logback-0.9.0.jar;%APP_HOME%\lib\logging-interceptor-4.8.1.jar;%APP_HOME%\lib\converter-moshi-2.9.0.jar;%APP_HOME%\lib\retrofit-2.9.0.jar;%APP_HOME%\lib\okhttp-4.8.1.jar;%APP_HOME%\lib\okio-jvm-2.7.0.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.7.20.jar;%APP_HOME%\lib\jackson-datatype-jsr310-2.13.4.jar;%APP_HOME%\lib\logstash-logback-encoder-7.2.jar;%APP_HOME%\lib\jackson-databind-2.13.4.jar;%APP_HOME%\lib\jackson-annotations-2.13.4.jar;%APP_HOME%\lib\jackson-core-2.13.4.jar;%APP_HOME%\lib\jackson-module-kotlin-2.13.4.jar;%APP_HOME%\lib\kotlin-reflect-1.7.0.jar;%APP_HOME%\lib\kotlin-stdlib-1.7.20.jar;%APP_HOME%\lib\kotlinx-coroutines-core-jvm-1.6.4.jar;%APP_HOME%\lib\kotlinx-coroutines-jdk8-1.6.4.jar;%APP_HOME%\lib\logback-classic-1.4.3.jar;%APP_HOME%\lib\kafka-clients-3.2.3.jar;%APP_HOME%\lib\slf4j-api-2.0.1.jar;%APP_HOME%\lib\msgpack-core-0.8.20.jar;%APP_HOME%\lib\checker-qual-3.5.0.jar;%APP_HOME%\lib\simpleclient-0.15.0.jar;%APP_HOME%\lib\joda-time-2.11.0.jar;%APP_HOME%\lib\moshi-1.8.0.jar;%APP_HOME%\lib\logback-core-1.4.3.jar;%APP_HOME%\lib\zstd-jni-1.5.2-1.jar;%APP_HOME%\lib\lz4-java-1.8.0.jar;%APP_HOME%\lib\snappy-java-1.1.8.4.jar;%APP_HOME%\lib\micrometer-core-1.9.4.jar;%APP_HOME%\lib\kotlin-stdlib-common-1.7.20.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\HdrHistogram-2.1.12.jar;%APP_HOME%\lib\LatencyUtils-2.0.3.jar;%APP_HOME%\lib\simpleclient_tracer_otel-0.15.0.jar;%APP_HOME%\lib\simpleclient_tracer_otel_agent-0.15.0.jar;%APP_HOME%\lib\config-1.4.2.jar;%APP_HOME%\lib\jansi-2.4.0.jar;%APP_HOME%\lib\simpleclient_tracer_common-0.15.0.jar


@rem Execute tms-utkast
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %TMS_UTKAST_OPTS%  -classpath "%CLASSPATH%" no.nav.tms.utkast.config.ApplicationKt %*

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable TMS_UTKAST_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%TMS_UTKAST_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
