package org.warcbase.pig;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.pig.Expression;
import org.apache.pig.FileInputLoadFunc;
import org.apache.pig.LoadMetadata;
import org.apache.pig.PigException;
import org.apache.pig.ResourceSchema;
import org.apache.pig.ResourceStatistics;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.apache.pig.impl.logicalLayer.schema.Schema.FieldSchema;
import org.jwat.arc.ArcRecordBase;
import org.warcbase.mapreduce.ArcInputFormat;

import com.google.common.collect.Lists;

public class ArcLoader extends FileInputLoadFunc implements LoadMetadata {
  private static final TupleFactory TUPLE_FACTORY = TupleFactory.getInstance();

  private RecordReader<LongWritable, ArcRecordBase> in;

  public ArcLoader() {
  }

  @Override
  public ArcInputFormat getInputFormat() throws IOException {
    return new ArcInputFormat();
  }

  @Override
  public Tuple getNext() throws IOException {
    try {
      if (!in.nextKeyValue()) {
        return null;
      }

      ArcRecordBase record = in.getCurrentValue();
      String type = record.getContentTypeStr();

      List<String> protoTuple = Lists.newArrayList();
      protoTuple.add(record.getUrlStr());
      protoTuple.add(record.getArchiveDateStr());
      protoTuple.add(type);

      // Only grab content text content, ignore binary data.
      if (type.toLowerCase().contains("text")) {
        protoTuple.add(new String(IOUtils.toByteArray(record.getPayloadContent()), 
            Charset.forName("UTF-8")));
      } else {
        // Otherwise add empty string to preserve consistent schema.
        protoTuple.add("");
      }

      return TUPLE_FACTORY.newTupleNoCopy(protoTuple);
    } catch (InterruptedException e) {
      int errCode = 6018;
      String errMsg = "Error while reading input";
      throw new ExecException(errMsg, errCode, PigException.REMOTE_ENVIRONMENT, e);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public void prepareToRead(RecordReader reader, PigSplit split) {
    in = reader;
  }

  @Override
  public void setLocation(String location, Job job) throws IOException {
    FileInputFormat.setInputPaths(job, location);
  }

  @Override
  public String[] getPartitionKeys(String location, Job job) throws IOException {
    return null;
  }

  @Override
  public ResourceSchema getSchema(String location, Job job) throws IOException {
    List<FieldSchema> fieldSchemaList = Lists.newArrayList();

    fieldSchemaList.add(new FieldSchema("url", DataType.CHARARRAY));
    fieldSchemaList.add(new FieldSchema("date", DataType.CHARARRAY));
    fieldSchemaList.add(new FieldSchema("mime", DataType.CHARARRAY));
    fieldSchemaList.add(new FieldSchema("content", DataType.CHARARRAY));

    return new ResourceSchema(new Schema(fieldSchemaList));
  }

  @Override
  public ResourceStatistics getStatistics(String location, Job job) throws IOException {
    return null;
  }

  @Override
  public void setPartitionFilter(Expression partitionFilter) throws IOException {
  }
}
