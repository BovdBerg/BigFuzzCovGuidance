package edu.tud.cs.jqf.bigfuzzplus.jsonMutation;

import com.smartentities.json.generator.GeneratorConfig;
import com.smartentities.json.generator.GeneratorFactory;
import com.smartentities.json.generator.generators.JsonValueGenerator;
import edu.tud.cs.jqf.bigfuzzplus.BigFuzzPlusMutation;
import org.everit.json.schema.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class JsonPlusMutation implements BigFuzzPlusMutation {
    Random r = new Random();
    ArrayList<String> fileRows = new ArrayList<>();
    JSONArray jsonRows = null;
    String delete;
    ArraySchema jsonSchema;
    int maxGenerateTimes = 20;


    @Override
    public void mutate(File inputFile, File nextInputFile) throws IOException {
        List<String> fileList = Files.readAllLines(Paths.get(inputFile.getPath()));
        if (!inputFile.getPath().contains("ref")) {
            setSchema(inputFile.getPath());
        }
        int n = r.nextInt(fileList.size());
        String fileToMutate = fileList.get(n);
        mutateFile(new File(fileToMutate));

        String fileName = nextInputFile.getPath(); //+ "+" + fileToMutate.substring(fileToMutate.lastIndexOf('/')+1);
        writeFile(new File(fileName), fileRows);

        String path = System.getProperty("user.dir") + "/" + fileName;
        delete = path;

        // write next ref file
        File refFile = new File(nextInputFile + "_ref");
        BufferedWriter bw = new BufferedWriter(new FileWriter(refFile));
        for(int i = 0; i < fileList.size(); i++)
        {
            if(i == n)
                bw.write(nextInputFile.getPath());
            else
                bw.write(fileList.get(i));
            bw.newLine();
            bw.flush();
        }
        bw.close();
    }

    /**
     * Sets the schema field for the JsonMutation class.
     * @param inputFile The filepath of the input seed, remark that this is not the schema filepath, this methods infers
     *                  the path to the schema.
     * @throws FileNotFoundException If the file does not exists, it throws an exception
     */
    private void setSchema(String inputFile) throws IOException {
        List<String> fileList = Files.readAllLines(Paths.get(inputFile));
        String file = fileList.get(0);
        String schemaFile = file;
        if (file.contains(".")) {
            schemaFile = file.substring(0, file.lastIndexOf('.')) + "_schema.json";
        }
        String schemaPath = schemaFile.substring(2);
        try {
            GeneratorConfig generatorConfig = GeneratorConfig.fromSchemaPath(schemaPath);
            jsonSchema = (ArraySchema) generatorConfig.getSchema();
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("Schema file not found. Please make sure there is also a json schema file present for the used input file");
        }
    }

    @Override
    public ArrayList<String> mutateFile(File inputFile) throws IOException {
        File file = inputFile;
        if (!file.exists()) {
            throw new IOException("File does not exist!");
        }
        String jsonString = new String(Files.readAllBytes(Paths.get(inputFile.getPath())));
        ArrayList<String> rows = new ArrayList<>();
        rows.add(jsonString);

        JSONParser p = new JSONParser();
        try {
            JSONArray arr = (JSONArray) p.parse(jsonString);
            setJsonRows(arr);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // 50/50 chance of generating extra rows
        if (r.nextBoolean()) {
            randomGenerateRows(rows);
//            System.out.println("rows: " + rows);
        }
        mutate(rows);
        fileRows = rows;
        return rows;
    }

    public JSONArray getJsonRows() {
        return jsonRows;
    }

    public void setJsonRows(JSONArray array) {
        jsonRows = array;
    }

    @Override
    public void mutate(ArrayList<String> rows) {
        JSONArray objects = getJsonRows();

//        System.out.println(objects.size());

        // Choose the object (row) to mutate
        int objNum = r.nextInt(objects.size()); // objectToMutate == colums from MutationTemplate
        String obj = objects.get(objNum).toString();
        JSONParser p = new JSONParser();
        JSONObject jsonToMutate = null;
        try {
            jsonToMutate = (JSONObject) p.parse(obj);
        } catch (ParseException e) {
            e.printStackTrace();
        }

//        System.out.println(jsonToMutate);

        // Choose the property (column) to mutate
//        JSONObject jsonToMutate = new JSONObject(objectToMutate);
        int propertyIndexToMutate = r.nextInt(jsonToMutate.keySet().size()); // propertyIndexToMutate == columnID
        Iterator<String> keyIterator = jsonToMutate.keySet().iterator();
        for (int i = 0; i < propertyIndexToMutate; i++) {
            keyIterator.next();
        }
        String propertyToMutate = keyIterator.next();

        int method = r.nextInt(6);
        ObjectSchema objectSchema = (ObjectSchema) jsonSchema.getItemSchemas().get(0);
        Schema valueSchema = objectSchema.getPropertySchemas().get(propertyToMutate);
        switch (method) {
//        Mutation options:
//        0: random change value   (M1)                     --> Generate new value of same type
//        1: random change into float (M2)                  --> Random change a value to a float (if value is an int)
//        2: random change delimiter (M3)                   --> Not possible with JSON
//        3: random insert character in column (M4)         --> Insert random character in value of a property
//        4: random delete one column (M5)                  --> Delete one property from JSON object
//        5: random change one column to empty string (M6)  --> Change one value to the empty string
            case 0: {
                // Generate new value of same type, but without bounds
                Object mutatedValue = generateValue(valueSchema, propertyToMutate);
                jsonToMutate.put(propertyToMutate, mutatedValue);
                break;
            }
            case 1: {
                // If the value is an int, change it to a float
                try {
                    NumberSchema sc = (NumberSchema) valueSchema;
                    if (sc.requiresInteger()) {
                        // Change the schema and generate a float instead of an int
                        NumberSchema.Builder builder = new NumberSchema.Builder();
                        builder.requiresInteger(false);
                        Number mutatedValue = (Number) GeneratorFactory.getGenerator(builder.build()).generate();
                        jsonToMutate.put(propertyToMutate, mutatedValue);
                    }
                    break;
                } catch (ClassCastException e) {
                    // Value is not even a number, continue
                    break;
                }
            }
            case 2: {
                break;
            }
            case 3: {
                // Insert random character in value of a property
                String mutatedValue = insertCharacter(jsonToMutate, propertyToMutate);
                jsonToMutate.put(propertyToMutate, mutatedValue);
                break;
            }
            case 4: {
                // Delete the random property from the json object
                jsonToMutate.remove(propertyToMutate);
                break;
            }
            case 5: {
                jsonToMutate.put(propertyToMutate, "");
                break;
            }
        }

        // Update the rows variable since something was mutated
        jsonRows.remove(objNum);
        jsonRows.add(jsonToMutate);
        setJsonRows(jsonRows);
        rows.clear();
        rows.add(jsonRows.toString());
    }

    @Override
    public void randomGenerateRows(ArrayList<String> rows) {
        int generatedTimes = r.nextInt(maxGenerateTimes)+1;
        JsonValueGenerator<?> gen = GeneratorFactory.getGenerator(jsonSchema.getItemSchemas().get(0));
        JSONArray newArray = getJsonRows();
        for (int i = 0; i < generatedTimes; i++) {
            newArray.add(gen.generate());
        }

        // Update the rows variable since it has generated new ones
        setJsonRows(newArray);
        rows.clear();
        rows.add(newArray.toString());
    }

    @Override
    public void writeFile(File outputFile, List<String> fileRows) throws IOException {
        File fout = outputFile;
        FileOutputStream fos = new FileOutputStream(fout);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        for (String fileRow : fileRows) {
            if (fileRow == null) {
                continue;
            }
            bw.write(fileRow);
            bw.newLine();
        }

        bw.close();
        fos.close();
    }

    @Override
    public void deleteFile(String currentFile) throws IOException {
        if (delete != null) {
            File del = new File(delete);
            del.delete();
        }
    }

    private String insertCharacter(JSONObject jsonToMutate, String propertyToMutate) {
        String value = jsonToMutate.get(propertyToMutate).toString();
        char randomCharacter = (char) r.nextInt(255);
        int randomIndex = r.nextInt(value.length());
        return value.substring(0,randomIndex) + randomCharacter + value.substring(randomIndex);
    }

    private Object generateValue(Schema valueSchema, String property) {
        if (valueSchema instanceof ArraySchema) {
            // Remove min and max items, keep all item schemas
            ArraySchema sc = (ArraySchema) valueSchema;
            List<Schema> itemSchemas = sc.getItemSchemas();
            ArraySchema.Builder builder = new ArraySchema.Builder();
            builder.maxItems(null);
            builder.minItems(null);
            for (Schema schema : itemSchemas) {
                builder.addItemSchema(schema);
            }
            ArraySchema newSchema = builder.build();
//            updateJsonSchema(newSchema, property);
            JSONArray out = new JSONArray();
            return out.add(GeneratorFactory.getGenerator(newSchema).generate());
        } else if (valueSchema instanceof BooleanSchema) {
            // Simply generate a new boolean
            BooleanSchema sc = (BooleanSchema) valueSchema;
            return GeneratorFactory.getGenerator(sc).generate();
        } else if (valueSchema instanceof EnumSchema) {
            // Simple generate a new enum value
            EnumSchema sc = (EnumSchema) valueSchema;
            return GeneratorFactory.getGenerator(sc).generate();
        } else if (valueSchema instanceof NumberSchema) {
            // Remove min, max and multiple, keep number/integer field
            NumberSchema sc = (NumberSchema) valueSchema;
            boolean reqInteger = sc.requiresInteger();
            NumberSchema.Builder builder = new NumberSchema.Builder();
            builder.minimum(null);
            builder.exclusiveMinimum(null);
            builder.exclusiveMinimum(false);
            builder.maximum(null);
            builder.exclusiveMaximum(null);
            builder.exclusiveMaximum(false);
            builder.multipleOf(null);
            builder.requiresInteger(reqInteger);
            NumberSchema newSchema = builder.build();
//            updateJsonSchema(newSchema, property);
            return GeneratorFactory.getGenerator(newSchema).generate();
        } else if (valueSchema instanceof ObjectSchema) {
            // Remove min and max properties, keep all property schemas
            ObjectSchema sc = (ObjectSchema) valueSchema;
            Map<String, Schema> propertySchemas = sc.getPropertySchemas();
            Iterator<String> it = propertySchemas.keySet().iterator();
            ObjectSchema.Builder builder = new ObjectSchema.Builder();
            builder.maxProperties(null);
            builder.minProperties(null);
            while (it.hasNext()) {
                String key = it.next();
                builder.addPropertySchema(key, propertySchemas.get(key));
            }
            ObjectSchema newSchema = builder.build();
//            updateJsonSchema(newSchema, property);
            String gen = GeneratorFactory.getGenerator(newSchema).generate().toString();
            JSONParser p = new JSONParser();
            JSONObject newObject = null;
            try {
                newObject = (JSONObject) p.parse(gen);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return newObject;
        } else if (valueSchema instanceof ReferenceSchema) {
            // Simply generate a new Reference
            ReferenceSchema sc = (ReferenceSchema) valueSchema;
            return GeneratorFactory.getGenerator(sc).generate();
        } else if (valueSchema instanceof StringSchema) {
            // Remove min and max length, keep pattern and format
            StringSchema sc = (StringSchema) valueSchema;

            String pattern = null;
            if (sc.getPattern() != null) {
                pattern = sc.getPattern().toString();
            }
            StringSchema.Builder builder = new StringSchema.Builder();
            builder.minLength(null);
            builder.maxLength(null);
            if (pattern != null) {
                builder.pattern(pattern);
            }
            StringSchema newSchema = builder.build();
//            updateJsonSchema(newSchema, property);
            return GeneratorFactory.getGenerator(newSchema).generate();
        } else {
            throw new IllegalStateException("Schema of value to mutate is invalid. Please check schema.");
        }
    }
}
