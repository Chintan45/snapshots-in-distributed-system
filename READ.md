# Snapshot Algorithms Implementation

This project implements two snapshot algorithms, Chandy-Lamport and Lai-Yang, using Akka actors in Scala. Each algorithm is implemented in a separate package within the project.

## Packages

1. **ChandyLamport**: Contains the implementation of the Chandy-Lamport snapshot algorithm.
   - To execute: `sbt "runMain com.snapshot.ChandyLamport.ChandyLamportSnapshot"`
   - It prompts the user to input `setup` to set up the Akka system and `read` to read transactions from the `transactions1.txt` file stored in the resources folder.
   - After successful execution, snapshots for each process are stored in the `snapshots` folder in JSON format.
<br/>
2. **LaiYang**: Contains the implementation of the Lai-Yang snapshot algorithm.
   - To execute: `sbt "runMain com.snapshot.LaiYang.LaiYangSnapshot"`
   - It prompts the user to input `setup` to set up the Akka system and `read` to read transactions from the `transactions2.txt` file stored in the resources folder.
   - After successful execution, snapshots for each process are stored in the `snapshots` folder in JSON format.

### Files Details
- **`ChandyLamportSnapshot.scala`**: Contains the overall implementation of Chandy Lamport Snapshot algorithm

- **`LaiYangSnapshot.scala`**: Contains the overall implementation of Lai Yang Snapshot algorithm

- **`GraphParser.scala`**: parses the `graph.ngs.dot` file to Graph

- **`FileUtils.scala`**: contains the functions for file handling

- **`MyLogger`**: Custom logger using slf4j library

### Usage

1. Clone the repository:
   ``` 
   git clone <repository_url> 
   ```
2. Navigate to the project directory:
    ```
    cd <project_directory>
    ```
3. Compile and run the Chandy-Lamport algorithm:
    ```
    sbt "runMain com.snapshot.ChandyLamport.ChandyLamportSnapshot"
    ```
4. Compile and run the Lai-Yang algorithm:
    ```
    sbt "runMain com.snapshot.LaiYang.LaiYangSnapshot"
    ```
5. Follow the prompts to set up the Akka system and read transactions from the files.
![instructions](./instructions.png)

6. After execution, find the snapshots stored in the snapshots folder for each process in json formate like
```
{
  "processId" : "2",
  "state" : {
    "02" : [ "m1", "m5", "m7"],
    "12" : [ "m2", "m3" ]
  }
}
```

### Dependencies
**Scala version**: 2.13.13
**sbt version**: 1.9.9
**Akka Actors**: 2.8.5
**ScalaTest**: 3.2.18
**Typesafe Config**: 1.4.3
**Play JSON**: 2.10.4
**Logback Classic**: 1.5.6
**SLF4J API**: 2.0.12

