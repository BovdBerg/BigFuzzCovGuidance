package edu.ucla.cs.jqf.bigfuzz.generation;

import edu.tud.cs.jqf.bigfuzzplus.BigFuzzPlusMutation;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static edu.tud.cs.jqf.bigfuzzplus.BigFuzzPlusDriver.PRINT_MUTATION_DETAILS;


public class CustomPlusMutation implements BigFuzzPlusMutation {

    Random r = new Random();
    String delete;


    @Override
    public void writeFile(File outputFile, List<String> fileRows) throws IOException {
        FileOutputStream fos = new FileOutputStream(outputFile);
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
    public void deleteFile(String currentInputFile) throws IOException {
        File del = new File(delete);
        del.delete();
    }

    @Override
    public void mutate(File inputFile, File nextInputFile) throws IOException
    {
        ArrayList<String> mutatedInput = mutateFile(inputFile);
        if (mutatedInput != null) {
            writeFile(nextInputFile, mutatedInput);
        }
        delete = nextInputFile.getPath();
    }

    @Override
    public ArrayList<String> mutateFile(File inputFile) throws IOException
    {
        ArrayList<String> rows = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(inputFile));

        if(inputFile.exists())
        {
            String readLine;
            while((readLine = br.readLine()) != null){
                rows.add(readLine);
            }
        }
        else
        {
            System.out.println("File does not exist!");
            return null;
        }

        int method =(int)(Math.random() * 2);
        if(method == 0){
            ArrayList<String> tempRows = new ArrayList<>();
            randomGenerateRows(tempRows);
            if (PRINT_MUTATION_DETAILS) { System.out.println("[MUTATE] rows: " + tempRows); }
            rows = tempRows;

            int next =(int)(Math.random() * 2);
            if(next == 0){
                mutate(rows);
            }
        }else{
            mutate(rows);
        }

        return rows;
    }

    public static String[] removeOneElement(String[] input, int index) {
        List result = new LinkedList();

        for(int i=0;i<input.length;i++)
        {
            if(i==index)
            {
                continue;
            }
            result.add(input[i]);
        }

        return (String [])result.toArray(input);
    }
    public static String[] AddOneElement(String[] input, String value, int index) {
        List result = new LinkedList();

        for(int i=0;i<input.length;i++)
        {
            result.add(input[i]);
            if(i==index)
            {
                result.add(value);
            }
        }

        return (String [])result.toArray(input);
    }

    public void mutate(ArrayList<String> list)
    {
        r.setSeed(System.currentTimeMillis());
        System.out.println("mutate size: " + list.size());
        int lineNum = r.nextInt(list.size());
        System.out.println("mutate linenum: " + list.get(lineNum));
        // 0: random change value
        // 1: random change into float
        // 2: random insert
        // 3: random delete one column
        // 4: random add one coumn
        String[] columns = list.get(lineNum).split("$del$");
        int method = r.nextInt(5);
        int columnID = r.nextInt(Integer.parseInt("$cols$"));
        System.out.println("Mutation *** "+method+" "+lineNum+" "+columnID);
        if(method == 0){
            columns[columnID] = Integer.toString(r.nextInt());
        }
        else if(method==1) {
            int value = 0;
            value = Integer.parseInt(columns[columnID]);
            float v = (float)value + r.nextFloat();
            columns[columnID] = Float.toString(v);
        }
        else if(method==2) {
            char temp = (char)r.nextInt(255);
            int pos = r.nextInt(columns[columnID].length());
            columns[columnID] = columns[columnID].substring(0, pos)+temp+columns[columnID].substring(pos);
        }
        else if(method==3) {
            columns = removeOneElement(columns, columnID);
        }
        else if(method==4) {
            String one = Integer.toString(r.nextInt(10000));
            columns = AddOneElement(columns, one, columnID);
        }
        String line = "";
        for(int j=0;j<columns.length;j++) {
            if(j==0)
            {
                line = columns[j];
            }
            else
            {
                line = line+","+columns[j];
            }
        }
        list.set(lineNum, line);
        /*for(int i=0;i<list.size();i++)
        {
            String line = list.get(i);
            String[] components = line.split(",");
            line = "";
            for(int j=0;j<components.length;j++)
            {
                if(r.nextDouble()>0.8)
                {
                    components[j] = randomChangeByte(components[j]);
                }
                if(line.equals(""))
                {
                    line = components[j];
                }
                else
                {
                    line = line+","+components[j];
                }
            }

            list.set(i, line);
        }*/
    }

    @Override
    public void randomGenerateRows(ArrayList<String> rows) {

    }

}
