package com.example.mobilemic.Network;
import java.util.LinkedList;
public class Protocol {

    /**
     * Reorders an array of MxN in clumpSizes s.t. the array becomes NxM e.g.
     * [0 1 2]	  [0 3 6]
     * [3 4 5] -> [1 4 7]
     * [6 7 8]	  [2 5 8]
     * @param a The array to shuffle
     * @param clumpSize The clump size to shuffle by.
     * @return Returns a reorder array (useful for delivering UDP packets and utilizing interpolation).
     */

    public static byte[][] reorder(byte[][] a, int clumpSize)
    {

        if (clumpSize > a.length)
            return rowsToCols(a);
        int rows = a[0].length;

        byte[][] retArray = new byte[rows][];
        byte[][] subArray = new byte[clumpSize][];
        LinkedList<byte[]> lSubArrays = new LinkedList<>();
        //Reorder sub arrays
        int count = 0;
        for (int i = 0; i < a.length; ++i)
        {
            //Reorder sub 2d array.
            //flawed logic.

            subArray[count] = a[i];

            //if count is of clump size - 1, do the shuffle.
            if (count >= clumpSize - 1)
            {
                //TODO:
                //rows to cols needs to change.
                byte[][] tempArray = shuffle(subArray);

                for (int k = 0; k < tempArray.length; ++k)
                    lSubArrays.add(tempArray[k]);

                //reset count
                count = 0;
            } else
                ++count;

        }

        //Convert list to array.

        return listToArray(lSubArrays);
    }

    /**
     * Shuffles the bytes by changing cols to row for an MxN matrix where M != N || M == N
     * @param a The array to shuffle
     * @return Returns the shuffled array.
     */
    private static byte[][] shuffle(byte[][] a)
    {
        int rows = a.length;
        int cols = 0;
        int u = 0;
        int v = 0;
        byte[][] retArray = new byte[rows][a[0].length];

        //Iterates through array and shuffles using my uhh crappy algorithm. :). PLS HIRE ME.
        for (int i =0; i < a.length; ++i)
        {

            for (int j = 0; j < a[i].length; ++j)
            {

                retArray[u++][v] = a[i][j];
                //If u is finally >= to num rows, increment v to start copying via columns
                if (u > rows - 1)
                {
                    ++v;
                    u = 0;
                }

            }

        }

        return retArray;
    }
    /**
     * Converts rows of a 2D array to the columns
     * @param a The array to convert
     * @return Returns an array where the columns and rows ahve been switched.
     */
    private static byte[][] rowsToCols(byte[][] a)
    {
        //Assume num cols is == for each row in a.
        int rows = a[0].length;
        int cols = 0;
        byte[][] retArray = new byte[rows][];

        for (int i = 0; i < rows; ++i)
        {
            cols = a.length;
            //Allocate enough cols for the array
            retArray[i] = new byte[cols];
            //Flip cols and rows.
            for (int j = 0; j < cols; ++j)
                retArray[i][j] = a[j][i];

        }
        return retArray;
    }

    /**
     * Converts a list of array bytes to a 2D array
     * @param lBytes The list to convert
     * @return Returns a 2D array containing the bytes.
     */

    private static byte[][] listToArray(LinkedList<byte[]> lBytes)
    {
        int rows = lBytes.size();
        byte[][] retArray = new byte[rows][];

        //remove first byte[] from the list
        for (int i = 0; !lBytes.isEmpty() && i < rows; ++i)
            retArray[i] = lBytes.remove();

        return retArray;
    }

    /**
     * Generates an array based on the specified num of cols and rows. Each value of the array is incremented by 1 (used for testing).
     * @param rows The number of rows
     * @param cols The number of cols.
     * @return Returns a new array whose values are incremented by 1 each.
     */
    private static byte[][] genArray(int rows, int cols)
    {
        byte[][] retArray = new byte[rows][cols];
        for (int i = 0; i < rows; ++i)
        {
            for (int j = 0; j < cols; ++j)
            {
                retArray[i][j] = (byte) ((i * cols) + j);
            }
        }
        return retArray;
    }

    /**
     * Prints the specified 2D array separated by a space.
     * @param array The 2D array to print.
     */

    public static void print2DArray(byte[][] array)
    {
        for (int i = 0; i < array.length; ++i)
        {
            for (int j = 0; j < array[i].length; ++j)
                System.out.print(array[i][j] + " ");
            System.out.println();

        }
    }
}


