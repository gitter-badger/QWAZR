Usage
=====

A description of the Web services provided by OpenSearchServer Graph.

List of Graph database 
----------------------

    curl -XGET http://localhost:9093
    
```json
[
	"base1",
	"base2",
	"base3"
]
```
    
Create a Graph database
-----------------------

	curl -XPOST -H "Content-Type:application/json" --data-binary @file.json http://localhost:9093/{db_name}

To create an item, just POST a JSON structure describing the database.

Replace {db_name} by the name of your database.

Here is the structure of a database:

```json
{
    "data": {
        "url": "http://localhost:8080",
        "name": "oss_graph",
        "login": "your_login",
        "api_key": "your_api_key",
        "time_out": 30000
    }, 
    "node_properties": {
        "type": "indexed",
        "date": "indexed",
        "name": "stored",
        "user": "stored"
    },
    "edge_types": [ "see", "buy"]
}
```

- **data**: specifies the OpenSearchServer backend instance and the index which contains the data.
- **node_properties**: the possible properties for a node. A property can be indexed or stored.
- **edge_types** : The possible type of edges. 


Get a database item
---------------------

    curl -XGET http://localhost:9093/{db_name}

Delete a database item
-----------------------

    curl -XDELETE http://localhost:9093/{db_name}
    
Insert or update a node
-----------------------

	curl -XPOST -H "Content-Type:application/json" --data-binary @file.json http://localhost:9093/{db_name}/node/{node_id}?upsert={true|false}
	
The parameters are:

- **db_name**: The name of the database
- **node_id**: The ID of the node
- **upsert**: If upsert is set to true, the properties and the edges will be added. If false (or missing) the properties and edges will be updated.

	
The structure of a node:

- **properties**: the properties attached to this node.
- **edges**: the connections with the other nodes.

An example:

```json
{
    "properties": {
        "type": "visit",
        "user": "john",
        "date": "20150115"
    },
    "edges": {
       "see": ["p1", "p2"],
       "buy": ["p1"]
    }
}
```

On this example, we store a node which represent the visit of "john" on our e-commerce web site.
John watched two products (p1 and p2) and he finally buy the product p1.

Insert or update several nodes
------------------------------

    curl -XPOST -H "Content-Type:application/json" --data-binary @file.json http://localhost:9093/{db_name}/node?upsert={true|false}
 
The parameters are:
   
- **db_name**: The name of the database
- **upsert**: If upsert is set to true, the properties and the edges will be added. If false (or missing) the properties and edges will be updated.

    
The structure is a hash. The key is the node_id. 

```json  
{
    "p1" :{
        "properties": {
            "type": "product",
            "name": "product1"
        }
    },
    "p2" :{
        "properties": {
            "type": "product",
            "name": "product2"
        }
	},
    "v1" :{
        "properties": {
            "type": "visit",
            "user": "john",
            "date": "20150115"
        },
        "edges": {
            "see": ["p1", "p2"],
            "buy": ["p1"]
        }
    }
}
```

Get a node
----------

	curl -XDELETE http://localhost:9093/{db_name}/node/{node_id}
	
```json
{
     "properties": {
        "type": "product",
        "name": "product1"
    }
}
```
	
Delete a node
-------------

	curl -XDELETE http://localhost:9093/{db_name}/node/{node_id}
	
The parameters are:

- **db_name**: the name of the graph database.
- **node_id**: the ID of the node.
	
Create / update an edge
-----------------------

	curl -XPOST http://localhost:9093/{db_name}/node/{node_id}/edge/{edge_type}/{to_node_id}

The parameters are:

- **db_name**: the name of the graph database.
- **node_id**: the ID of the node.
- **edge_type**: the type of the edge.
- **to_node_id**: the ID of the related node.
	
Delete an edge
--------------

	curl -XDELETE http://localhost:9093/{db_name}/node/{from_node_id}/edge/{edge_type}/{to_node_id}

The parameters are:

- **db_name**: the name of the graph database.
- **from_node_id**: the ID of the origin node.
- **edge_type**: the type of the edge.
- **to_node_id**: the ID of the related node.

Make a graph request
--------------------

To create an item, just POST a JSON structure describing the request.

	curl -XPOST -H "Content-Type:application/json" --data-binary @file.json http://localhost:9093/{db_name}/request

The parameters are:

- **db_name**: the name of the graph database.
- **payload**: A request in JSON format 

Here is the structure of a request:

```json
{
    "start": 0,
    "rows": 3,
    "edge_type_weight": {
        "see": 0.1,
        "buy": 2
    },
    "exclude_nodes": [ "p133", "p73", "p77", "p44", "p155", "p135"],
    "edges": {
        "see": ["p133", "p73", "p77", "p44", "p155", "p135"]
    }
}
```

- **start**: Manage the pagination.
- **rows**: The number of nodes returned.
- **edge_type_weight**: An optional weight for each type of edge.
- **exclude_nodes**: An array of nodes ID. This nodes will be excluded from the result.
- **edges**: The edges of the node to compare

The result is an array of nodes.

```json
[ {
  "properties" : {
    "name" : "product10",
    "type" : "product"
  },
  "score" : 14.3,
  "node_id" : "p10"
}, {
  "properties" : {
    "name" : "product153",
    "type" : "product"
  },
  "score" : 13.700000000000001,
  "node_id" : "p153"
}, {
  "properties" : {
    "name" : "product327",
    "type" : "product"
  },
  "score" : 13.700000000000001,
  "node_id" : "p327"
} ]
```
