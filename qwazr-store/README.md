QWAZR Store
===========

A smart distributed file server for [QWAZR](https://www.qwazr.com).


**Warning! This document is a draft:**
The store service is still under heavy developement.
Therefore this documentation is not final.

The Store module is a multi-master distributed file system storage system. It is mainly a REST/JSON web service. The requests must be submitted to a master node.

Store contains a set of **schemas**. Each files stored in a schema gets distributed and replicated across the nodes of the cluster. A schema is defined by three parameters:

- The name of the schema.
- The distribution factor defines on how many nodes across the cluster the file will be distributed.
- The replication factor defines how many copies of the same file are created across the cluster.

# Usage

## Creating a schema

```bash
curl -X POST -H "Content-Type: application/json" \
    -d '{ "replication_factor": 1, "distribution_factor": 1 }' \
    "http://localhost:9091/store_shema/my_schema"
```

## Uploading a file

To upload a file, use an HTTP PUT command. The payload of the request is the file.

```bash
curl -X PUT -T my_file.png "http://localhost:9091/store/my_schema/my_subdir/my_file.png"
```

If the file already exists, it is overwritten.
If the path contains non-existent directories, they are automatically created.
The file is stored in the cluster in compliance with the schema's parameters (applying both the distribution and replication factors).

## Display the directory content

A simple GET method will display the content of the directory:

```bash
curl "http://localhost:9091/store/my_schema/my_subdir"
```

The server returns the content using the JSON format:

```json
{
  "type" : "DIRECTORY",
  "lastModified" : "2015-05-15T16:34:22.000+0000",
  "childs" : {
    "my_file.png" : {
      "type" : "FILE",
      "lastModified" : "2015-05-15T16:34:13.000+0000",
      "size" : 26785
    },
    "my_subdir" : {
      "type" : "DIRECTORY",
      "lastModified" : "2015-05-15T16:34:22.000+0000"
    }
  }
}
```

## Downloading a file

The GET method will return the content of the file.
The content is returned with the **application/octet-stream** content type.

```bash
curl "http://localhost:9091/store/my_schema/my_subdir/my_file.png"
```

## Deleting a file

The DELETE HTTP method will delete a file from the cluster.

```bash
curl -X DELETE "http://localhost:9091/store/my_schema/my_subdir/my_file.png"
```

After deleting a file, if the parent directory is empty, it is automatically deleted.



- - -

QWAZR is a an application server dedicated to distributed projects.
The server provides a set of JSON web services which manages distributed jobs and
web applications written in Javascript or Java.

The main [documentation of QWAZR is available here](https://github.com/qwazr/QWAZR/wiki).