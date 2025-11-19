# In-Memory File System - LLD Interview Problem

## 📋 Problem Statement

Design an in-memory file system that:
- Create/delete files and directories
- Navigate paths
- List directory contents
- Read/write file content
- Move/copy operations

## 🎯 Key Requirements

### Functional Requirements:
1. mkdir - Create directory
2. touch - Create file
3. cd - Change directory
4. ls - List contents
5. rm - Delete file/directory
6. read/write - File operations

### Non-Functional Requirements:
1. Efficient path navigation
2. Tree structure
3. Memory efficient

## 🏗️ Design Components

### Core Classes:
1. **FileSystem** - Main system
2. **Node** - File/Directory
3. **File** - File node
4. **Directory** - Directory node

## 🎨 Design Patterns Used

1. **Composite Pattern** - File/Directory tree
2. **Singleton Pattern** - Single FS instance

## 📊 Complexity

- Create: O(path length)
- Delete: O(path length)
- List: O(children)
- Space: O(total nodes)
