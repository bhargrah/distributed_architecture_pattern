# Architecture Pattern
Monorepo for Architecture Pattern Notes and Code Samples 

Repo will have below structure 

- Distributed Pattern Code Replicate [Forked from https://github.com/unmeshjoshi/replicate]
- Caching Patterns
- Communication Pattern

# Repo Structure 

### Replicate 
 - This repo has all the code samples related to distributed algorithms. 
 - All the algos needs to be studies from this repo. 

### Durable Cache MS
 - This repo will have implementation of algorithms that are discussed in Replicate repo.
 - As the very first task , **Write Ahead Log** (WAL) is implemented.  


## Write Ahead Log  - Usage 
 - Consensus algorithm like Zookeeper and RAFT is similar to write ahead log. 
 - Kafka follows similar structure as that of commit logs in database.
 - Every database like Postgres or Cassandra implements write ahead log. 
