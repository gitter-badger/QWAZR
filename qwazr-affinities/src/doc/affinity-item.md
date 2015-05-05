Structure of an Affinity item
=============================

- "data": specifies the OpenSearchServer instance and the index which contains the data.
- "crawl": specifies the OpenSearchServer instance and the index used for the web crawl.
- "cache": specifies the OpenSearchServer instance and the index used for results caching.
- "type": specifies the affinity method: EXACT_MATCH or SCORING.
- "criteria": the tested fields with their weights.
- "returned_fields" : The fields which are finally returned for matching documents. 

An "exact match" affinity item with one criterion
-------------------------------------------------

```json
{
    "data": {
        "url": "http://ssd7.open-search-server.com/myinstance/",
        "api_key": "88a8834964774a2b53c288e5b20f6e17",
        "login": "mylogin",
        "name": "data-index"
    },
    "type":"EXACT_MATCH",
    "criteria": {
        "ean": 1
    },
    "returned_fields": [ "id", "title" ]
}
```

An "exact match" affinity item with two criteria
------------------------------------------------


```json
{
    "data": {
        "url": "http://ssd7.open-search-server.com/myinstance/",
        "api_key": "88a8834964774a2b53c288e5b20f6e17",
        "login": "mylogin",
        "name": "data-index"
    },
    "type":"EXACT_MATCH",
    "criteria": {
        "provider_id": 1,
        "product_id": 1
    },
    "returned_fields": [ "id", "tile" ]
}
```

An approaching affinity item with criteria
------------------------------------------

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

An approaching affinity item with criteria, web crawling and cache
------------------------------------------------------------------
 
```json
{
    "crawl": {
        "url": "http://ssd7.open-search-server.com/myinstance/",
        "api_key": "88a8834964774a2b53c288e5b20f6e17",
        "login": "mylogin",
        "name": "crawl-index"
    },
    "data": {
        "url": "http://ssd7.open-search-server.com/myinstance/",
        "api_key": "88a8834964774a2b53c288e5b20f6e17",
        "login": "mylogin",
        "name": "data-index"
    },
    "cache": {
        "url": "http://ssd7.open-search-server.com/myinstance/",
        "api_key": "88a8834964774a2b53c288e5b20f6e17",
        "login": "mylogin",
        "name": "cache-index"
    },
    "crawl_mapping": {
        "title": "title",
        "full" : "content"
    },
    "type":"SCORING",
    "criteria": {
        "title": 10,
        "full": 1
    },
    "returned_fields": [ "id", "tile" ]
}
```