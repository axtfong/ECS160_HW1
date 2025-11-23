package com.ecs160.hw2.persistence;

import redis.clients.jedis.Jedis;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Redis persistence framework that uses annotations to persist and load objects.
 */
public class RedisDB {
    private Jedis jedis;
    private int defaultDatabase;
    private SimpleDateFormat dateFormat;

    public RedisDB() {
        this("localhost", 6379, 0);
    }

    public RedisDB(String host, int port, int database) {
        this.jedis = new Jedis(host, port);
        this.defaultDatabase = database;
        this.jedis.select(database);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    /**
     * Persists an object to Redis. Only fields annotated with @PersistableField will be persisted.
     * Handles nested objects and List collections.
     */
    public boolean persist(Object o) {
        if (o == null) {
            return false;
        }

        try {
            Class<?> clazz = o.getClass();
            
            // Check if class is annotated with @PersistableObject
            if (!clazz.isAnnotationPresent(PersistableObject.class)) {
                return false;
            }

            // Find the @Id field
            Field idField = findIdField(clazz);
            if (idField == null) {
                throw new RuntimeException("Class " + clazz.getName() + " must have a field annotated with @Id");
            }

            idField.setAccessible(true);
            Object idValue = idField.get(o);
            if (idValue == null) {
                throw new RuntimeException("Id field cannot be null for class " + clazz.getName());
            }

            String objectKey = idValue.toString();
            String className = clazz.getName();

            // Persist the object's fields
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(PersistableField.class)) {
                    field.setAccessible(true);
                    Object fieldValue = field.get(o);

                    if (fieldValue == null) {
                        jedis.hset(objectKey, field.getName(), "");
                        continue;
                    }

                    Class<?> fieldType = field.getType();

                    // Handle List collections
                    if (List.class.isAssignableFrom(fieldType)) {
                        String redisKey = mapFieldNameToRedis(field.getName());
                        persistList(objectKey, redisKey, (List<?>) fieldValue);
                    }
                    // Handle nested objects (must also be @PersistableObject)
                    else if (fieldType.isAnnotationPresent(PersistableObject.class)) {
                        String redisKey = mapFieldNameToRedis(field.getName());
                        persist(objectKey, redisKey, fieldValue);
                    }
                    // Handle primitive types and strings
                    else {
                        String valueStr = convertToString(fieldValue);
                        String redisKey = mapFieldNameToRedis(field.getName());
                        jedis.hset(objectKey, redisKey, valueStr);
                    }
                }
            }

            // Store class name for later loading
            jedis.hset(objectKey, "_class", className);

            return true;
        } catch (Exception e) {
            System.err.println("Error persisting object: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Helper method to persist nested objects or objects in different Redis keys.
     */
    private void persist(String parentKey, String fieldName, Object nestedObject) {
        if (nestedObject == null) {
            jedis.hset(parentKey, fieldName, "");
            return;
        }

        // Persist the nested object (recursively)
        persist(nestedObject);
        
        // Store reference to the nested object using its ID
        Field idField = findIdField(nestedObject.getClass());
        if (idField != null) {
            idField.setAccessible(true);
            try {
                Object nestedId = idField.get(nestedObject);
                jedis.hset(parentKey, fieldName, nestedId.toString());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper method to persist List collections.
     */
    private void persistList(String parentKey, String fieldName, List<?> list) {
        if (list == null || list.isEmpty()) {
            jedis.hset(parentKey, fieldName, "");
            return;
        }

        List<String> itemIds = new ArrayList<>();
        
        for (Object item : list) {
            if (item == null) {
                continue;
            }

            // If item is a @PersistableObject, persist it and store its ID
            if (item.getClass().isAnnotationPresent(PersistableObject.class)) {
                persist(item);
                
                Field idField = findIdField(item.getClass());
                if (idField != null) {
                    idField.setAccessible(true);
                    try {
                        Object itemId = idField.get(item);
                        if (itemId != null) {
                            itemIds.add(itemId.toString());
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // For primitive types in lists, store them directly
                itemIds.add(convertToString(item));
            }
        }

        // Store comma-separated list of IDs or values
        String listValue = String.join(",", itemIds);
        jedis.hset(parentKey, fieldName, listValue);
    }

    /**
     * Loads an object from Redis. The object must have its @Id field populated.
     * Handles nested objects and List collections.
     */
    public Object load(Object o) {
        if (o == null) {
            return null;
        }

        try {
            Class<?> clazz = o.getClass();
            
            // Check if class is annotated with @PersistableObject
            if (!clazz.isAnnotationPresent(PersistableObject.class)) {
                return null;
            }

            // Find the @Id field
            Field idField = findIdField(clazz);
            if (idField == null) {
                throw new RuntimeException("Class " + clazz.getName() + " must have a field annotated with @Id");
            }

            idField.setAccessible(true);
            Object idValue = idField.get(o);
            if (idValue == null) {
                throw new RuntimeException("Id field cannot be null for class " + clazz.getName());
            }

            String objectKey = idValue.toString();

            // Check if the object exists in Redis
            if (!jedis.exists(objectKey)) {
                return null;
            }

            // Create a new instance
            Object instance = clazz.getDeclaredConstructor().newInstance();

            // Load all fields
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(PersistableField.class)) {
                    field.setAccessible(true);
                    
                    // Check if this field is lazy loaded
                    if (isLazyLoaded(clazz, field.getName())) {
                        // Don't load lazy fields immediately
                        continue;
                    }

                    String redisKey = mapFieldNameToRedis(field.getName());
                    String fieldValueStr = jedis.hget(objectKey, redisKey);
                    if (fieldValueStr == null || fieldValueStr.isEmpty()) {
                        // Try with original field name as fallback
                        fieldValueStr = jedis.hget(objectKey, field.getName());
                    }
                    if (fieldValueStr == null || fieldValueStr.isEmpty()) {
                        continue;
                    }

                    Class<?> fieldType = field.getType();

                    // Handle List collections
                    if (List.class.isAssignableFrom(fieldType)) {
                        List<?> loadedList = loadList(objectKey, redisKey, field);
                        field.set(instance, loadedList);
                    }
                    // Handle nested objects
                    else if (fieldType.isAnnotationPresent(PersistableObject.class)) {
                        Object nestedObject = loadNested(fieldType, fieldValueStr);
                        field.set(instance, nestedObject);
                    }
                    // Handle primitive types and strings
                    else {
                        Object convertedValue = convertFromString(fieldValueStr, fieldType);
                        field.set(instance, convertedValue);
                    }
                }
            }

            // Set the ID field
            idField.set(instance, idValue);

            return instance;
        } catch (Exception e) {
            System.err.println("Error loading object: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Helper method to check if a field is marked as lazy loaded.
     */
    private boolean isLazyLoaded(Class<?> clazz, String fieldName) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(LazyLoad.class)) {
                LazyLoad annotation = method.getAnnotation(LazyLoad.class);
                if (annotation.field().equals(fieldName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method to load a List collection.
     */
    private List<?> loadList(String parentKey, String fieldName, Field field) {
        String listValueStr = jedis.hget(parentKey, fieldName);
        if (listValueStr == null || listValueStr.isEmpty()) {
            return new ArrayList<>();
        }

        String[] itemIds = listValueStr.split(",");
        List<Object> list = new ArrayList<>();

        // Get the generic type of the List
        ParameterizedType listType = (ParameterizedType) field.getGenericType();
        Class<?> itemType = (Class<?>) listType.getActualTypeArguments()[0];

        for (String itemId : itemIds) {
            if (itemId == null || itemId.isEmpty()) {
                continue;
            }

            // If the item type is a @PersistableObject, load it
            if (itemType.isAnnotationPresent(PersistableObject.class)) {
                try {
                    Object item = itemType.getDeclaredConstructor().newInstance();
                    Field idField = findIdField(itemType);
                    if (idField != null) {
                        idField.setAccessible(true);
                        // Try to parse as the ID field type
                        Object idValue = convertFromString(itemId, idField.getType());
                        idField.set(item, idValue);
                        Object loadedItem = load(item);
                        if (loadedItem != null) {
                            list.add(loadedItem);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // For primitive types, convert directly
                Object convertedValue = convertFromString(itemId, itemType);
                list.add(convertedValue);
            }
        }

        return list;
    }

    /**
     * Helper method to load a nested object.
     */
    private Object loadNested(Class<?> nestedType, String nestedId) {
        try {
            Object nestedInstance = nestedType.getDeclaredConstructor().newInstance();
            Field idField = findIdField(nestedType);
            if (idField != null) {
                idField.setAccessible(true);
                // Try to parse as the ID field type
                Object idValue = convertFromString(nestedId, idField.getType());
                idField.set(nestedInstance, idValue);
                return load(nestedInstance);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Helper method to find the field annotated with @Id.
     */
    private Field findIdField(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Id.class)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Convert an object to a string representation for storage.
     */
    private String convertToString(Object value) {
        if (value == null) {
            return "";
        }
        
        if (value instanceof Date) {
            return dateFormat.format((Date) value);
        }
        
        return value.toString();
    }

    /**
     * Convert a string to the appropriate type.
     */
    private Object convertFromString(String str, Class<?> targetType) {
        if (str == null || str.isEmpty()) {
            return null;
        }

        if (targetType == String.class) {
            return str;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(str);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(str);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(str);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(str);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(str);
        } else if (targetType == Date.class) {
            try {
                return dateFormat.parse(str);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        return str;
    }

    /**
     * Maps Java field names to Redis field names.
     * Handles special cases like "authorName" -> "Author Name"
     */
    private String mapFieldNameToRedis(String javaFieldName) {
        // Handle special mappings
        if ("authorName".equals(javaFieldName)) {
            return "Author Name";
        }
        // Handle camelCase to Title Case mapping for Date
        if ("date".equalsIgnoreCase(javaFieldName)) {
            return "Date";
        }
        if ("description".equalsIgnoreCase(javaFieldName)) {
            return "Description";
        }
        // Add other mappings as needed
        return javaFieldName;
    }
    
    /**
     * Maps Redis field names to Java field names.
     */
    private String mapRedisToFieldName(String redisFieldName) {
        if ("Author Name".equals(redisFieldName)) {
            return "authorName";
        }
        if ("Date".equals(redisFieldName)) {
            return "date";
        }
        return redisFieldName;
    }

    /**
     * Close the Redis connection.
     */
    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }
}

