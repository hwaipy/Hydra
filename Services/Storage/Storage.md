# StorageService


## Feature

StorageService is one of the core service in Hydra. StorageService offers file based storage of any Data and Registration. The files are organized by Paths, just similar to common operation systems.

A simple way to access the stored data is through [DataViewer](/apps/dataviewer.html).

--------

## Build-in File Formats

##### Hydra Binary Table (HBT)

An HBT is a single file in StorageService. The filename of an HBT file should be end with ".hbt". Columns are defined at the creation of the file. Rows can be append anytime. Currently the data type can only be Numbers, including Byte, Short, Int, Long, Float and Double. Boolean and String will be added soon. The data type should be the same for each column.

##### Hydra Individual Package (HIP)

An HIP is a single directory in StorageService. The filename of an HIP directory should be end with ".hip". HIP is used to organize closely related files, such as several HBTs in a single trial. 

##### Note

Note can be regarded as metadata of any Directory. 

-------

## API

StorageService is designed to have authentication mechanism to control the permission of access. However in the current version the authentication system is not finished. Thus, for almost every function, an ```user``` parameter is defined but not used. ```user``` should be assigned for ```""```.

Each element have a Type, which could be ```Collection``` (equivalent to Directory), ```Content``` (equivalent to File), ```NotExist``` or ```Unknown```.

```
listElements(user: String, path: String, withMetaData: Boolean = false)
```

This function list all files contains by ```path```. ```path``` should be a Directory. If ```withMetaData``` is ```false```, this function returns a list of String, contains the filename of every file. Else, this function returns a list of Map. Each Map contains the metadata of each file. See function ```metaData``` for details of the Map.

```metaData(user: String, path: String, withTime: Boolean = false)```

Return a Map of metaData related to ```path```. The Map contains Name, Path, and Type (can be ```Collection```, ```Content```, ```NotExist``` or ```Unknown```) of ```path```. If ```withTime``` is ```true```, CreationTime, LastAccessTime, and LastModifiedTime are included. If Type is ```Content```, the Size is included.

```read(user: String, path: String, start: Long, length: Int)```

Return a Byte Array. ```path``` should be a File.

```readAll(user: String, path: String)```

Read the entire file and returns a Byte Array. ```path``` should be a File.

```append(user: String, path: String, data: Array[Byte])```

Append ```data``` to ```path```. The Type of ```path``` should be ```Content```.

```write(user: String, path: String, data: Array[Byte], start: Long)```

Write ```data``` to ```path``` at position ```start```. The Type of ```path``` should be ```Content```.

```clear(user: String, path: String)```

Clear the data in ```path```.  The Type of ```path``` should be ```Content```. After invoke, the Size of ```path``` will be set to 0.

```delete(user: String, path: String)```

Move ```path``` to trash.

```readNote(user: String, path: String)```

Return a String. The Type of ```path``` should be ```Collection```.

```writeNote(user: String, path: String, data: String)```

Rewrite Note with ```data```. The Type of ```path``` should be ```Collection```.

```createFile(user: String, path: String)```

The Type of ```path``` should be ```NotExist``` before invoke. After invoke, the Type will be ```Content```.

```createDirectory(user: String, path: String)```

The Type of ```path``` should be ```NotExist``` before invoke. After invoke, the Type will be ```Collection```.

```exists(user: String, path: String)```

Check if the Type of ```path``` is ```Collection``` or ```Content```. Return a Boolean.

```getHipInformation(user: String, path: String)```

The Type of ```path``` should be ```Collection```, and the Name of ```path``` should be end with ".hip". Returns a Map containing key information on the HIP. Equivalent to ```Map("note" -> readNote(user, path), "items" -> listElements(user, path, true))```

```HBTFileInitialize(user: String, path: String, heads: List[List[String]])```

Initialize ```path``` to a specified HBT File. ```path``` can be ```NotExist``` or ```Content``` with Size of 0. The name of ```path``` should be end with ".hbt". Each element in ```heads``` specified a column. Each element in ```head``` should be a List of String with exactly 2 element, with the first one define DataType (Byte, Short, Int, Long, Float or Double), and the second one define Title of column.

```HBTFileMetaData(user: String, path: String)```

Return a Map contains key information of a HBT File, including ColumnCount, RowDataLength, RowCount, and Heads.

```HBTFileAppendRows(user: String, path: String, rowsData: List[List[Any]])```

Append rows to ```path```. Each element in ```rowsData``` specified one row of data. The type of data should match the definition of the HBT columns.

```HBTFileAppendRow(user: String, path: String, rowData: List[Any])```

Append a single row to ```path```. The type of data should match the definition of the HBT columns.

```HBTFileReadRows(user: String, path: String, from: Int, count: Int)```

Return ```List[List[Any]]``` that corresponding to the data in ```path```.

```HBTFileReadAllRows(user: String, path: String)```

Return ```List[List[Any]]``` that corresponding to the data in ```path```.

--------

_Author: Hwaipy_

_Version: 0.3.0_

_Date: 17 Jun. 2018_


