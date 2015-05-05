Usage
=====

A description of the Web services provided by OpenSearchServer Affinities.

List of Affinity items 
----------------------

    curl -XGET http://localhost:9092
    
```json
[
	"book-exact",
	"book-id",
	"book-score"
]
```
    
Create an Affinity item
-----------------------

	curl -XPOST -H "Content-Type:application/json" --data-binary @file.json http://localhost:9092/{item-name}

To create an item, just POST a JSON structure describing the item.

Replace {item-name} by the name of your item.

[Structure of an Affinity item](affinity-item.md)


Get an Affinity item
---------------------

    curl -XGET http://localhost:9092/{item-name}
    
```json
{
    "data": {
        "url": "http://ssd7.open-search-server.com/myinstance/",
        "api_key": "88a8834964774a2b53c288e5b20f6e17",
        "login": "mylogin",
        "name": "data-index"
    },
    "type":"SCORING",
    "criteria": {
        "name": 5,
       	"product_id": 5,
       	"full": 1
    },
    "returned_fields": [ "id", "tile" ]
}
```

Delete an Affinity item
-----------------------

    curl -XDELETE http://localhost:9092/{item-name}
    
Execute an affinity request
---------------------------

    curl -XPOST -H "Content-Type:application/json" --data-binary @file.json http://localhost:9092/{item-name}/request

POST a JSON structure with the criteria and the parameters.

Replace {item-name} by the name of your item.
 
 ```json
 {
    "criteria": {
        "name": "John Smith",
        "full": "My fabulous content"
    },
    "cache": false
}
```

Execute a batch of requests
---------------------------

You can execute a list of Affinity requests. 

    curl -XPOST -H "Content-Type:application/json" --data-binary @file.json http://localhost:9092/requests

POST a JSON array of affinity requests.

```json
[
    {
	    "name": "book-exact",
		"criteria": {
        	"ean": "97829531837950000000"
		},
		"action": "CONTINUE"
	},
	{
	    "name": "book-score",
    	"criteria": {
        	"name": "John Smith",
        	"full": "My fabulous content"
    	},
    	"action": "STOP_IF_FOUND"
	},
	{
	    "name": "book-score",
		 "url":"http://www.lalettrine.com/article-la-route-qui-mene-a-la-ville---natalia-ginzburg-125150206.html"
	}
]
```

Execute a batch of batch of requests
------------------------------------

You can execute a list of batch of Affinity requests. 

	curl -XPOST -H "Content-Type:application/json" --data-binary @file.json http://localhost:9092/batch/requests

```json
[
  [
    {
	    "name": "book-exact",
		"criteria": {
        	"ean": "97829531837950000000"
		},
		"action": "CONTINUE"
	},
	{
	    "name": "book-score",
    	"criteria": {
        	"name": "John Smith",
        	"full": "My fabulous content"
    	},
    	"action": "STOP_IF_FOUND"
	},
	{
	    "name": "book-score",
		 "url":"http://www.lalettrine.com/article-la-route-qui-mene-a-la-ville---natalia-ginzburg-125150206.html"
	}
  ],
  [
    {
	    "name": "book-exact",
		"criteria": {
        	"ean": "95318978200379500000"
		},
		"action": "CONTINUE"
	},
	{
	    "name": "book-score",
    	"criteria": {
        	"name": "Jack London",
        	"full": "My other book"
    	},
    	"action": "STOP_IF_FOUND"
	},
	{
	    "name": "book-score",
		 "url":"http://www.lalettrine.com/article-la-route-qui-mene-a-la-ville---natalia-ginzburg-125150206.html"
	}
  ]
]
```