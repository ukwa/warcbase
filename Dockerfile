FROM ubuntu:16.04
MAINTAINER Andrew.Jackson@bl.uk

ENV TERM xterm

RUN apt-get update && apt-get install -y \
    apt-transport-https \
    curl \
    openjdk-8-jdk \
    vim \
    wget \
    maven \
  && rm -rf /var/lib/apt/lists/*

ENV SPARK_VERSION=1.6.2 \
    HADOOP_VERSION=2.6 \
    SCALA_VERSION=2.10.5

# Scala and SBT
RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list \
  && apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823 \
  && apt-get update && apt-get install -y scala sbt \
  && rm -rf /var/lib/apt/lists/*

# Spark
RUN wget http://www-us.apache.org/dist/spark/spark-$SPARK_VERSION/spark-$SPARK_VERSION-bin-hadoop$HADOOP_VERSION.tgz \
  && tar xfz spark-$SPARK_VERSION-bin-hadoop$HADOOP_VERSION.tgz \
  && rm spark-$SPARK_VERSION-bin-hadoop$HADOOP_VERSION.tgz

# Spark Notebook
#RUN wget https://s3.eu-central-1.amazonaws.com/spark-notebook/tgz/spark-notebook-master-scala-$SCALA_VERSION-spark-$SPARK_VERSION-hadoop-$HADOOP_VERSION.0-cdh5.4.2.tgz \
#  && tar xfz spark-notebook-master-scala-$SCALA_VERSION-spark-$SPARK_VERSION-hadoop-$HADOOP_VERSION.tgz \
#  && rm spark-notebook-master-scala-$SCALA_VERSION-spark-$SPARK_VERSION-hadoop-$HADOOP_VERSION.tgz
ENV SPARK_NOTEBOOK_VERSION=0.7.0-scala-2.11.8-spark-2.1.0-hadoop-2.6.0
#RUN wget https://s3.eu-central-1.amazonaws.com/spark-notebook/tgz/spark-notebook-master-scala-2.10.5-spark-1.6.2-hadoop-2.6.0-cdh5.4.2.tgz
# https://s3.eu-central-1.amazonaws.com/spark-notebook/tgz/spark-notebook-0.6.3-scala-2.10.5-spark-1.6.2-hadoop-2.6.0-cdh5.7.1-with-hive-with-parquet.tgz?max-keys=100000
RUN wget https://s3.eu-central-1.amazonaws.com/spark-notebook/tgz/spark-notebook-$SPARK_NOTEBOOK_VERSION.tgz \
  && tar xfz spark-notebook-$SPARK_NOTEBOOK_VERSION.tgz \
  && rm spark-notebook-$SPARK_NOTEBOOK_VERSION.tgz

# Warcbase
COPY . /warcbase
RUN cd /warcbase \
  && mvn install

ENV ADD_JARS /warcbase/warcbase-core/target/warcbase-core-0.1.0-SNAPSHOT-fatjar.jar

WORKDIR /work

EXPOSE 8080
VOLUME /deps /data /notes

#COPY zeppelin-env.sh conf/zeppelin-env.sh.temp
#RUN tr "\r" " " < conf/zeppelin-env.sh.temp > conf/zeppelin-env.sh

#CMD bin/zeppelin-daemon.sh start && bash
CMD /spark-notebook-$SPARK_NOTEBOOK_VERSION/bin/spark-notebook

