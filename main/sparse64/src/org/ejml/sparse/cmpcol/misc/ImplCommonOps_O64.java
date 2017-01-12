/*
 * Copyright (c) 2009-2017, Peter Abeles. All Rights Reserved.
 *
 * This file is part of Efficient Java Matrix Library (EJML).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ejml.sparse.cmpcol.misc;

import org.ejml.data.SMatrixCC_F64;
import org.ejml.sparse.cmpcol.CommonOps_O64;

import java.util.Arrays;

/**
 * Implementation class.  Not recommended for direct use.  Instead use {@link CommonOps_O64}
 * instead.
 *
 * @author Peter Abeles
 */
public class ImplCommonOps_O64 {

    /**
     * Performs a matrix transpose.
     *
     * @param A Original matrix.  Not modified.
     * @param C Storage for transposed 'a'.  Assumed to be of the correct shape and length.
     * @param work Work space.  null or an array the size of the rows in 'a'
     */
    public static void transpose(SMatrixCC_F64 A , SMatrixCC_F64 C , int work[] ) {
        work = checkDeclareRows(A, work, true);
        C.length = A.length;

        // compute the histogram for each row in 'a'
        int idx0 = A.col_idx[0];
        for (int j = 1; j <= A.numCols; j++) {
            int idx1 = A.col_idx[j];
            for (int i = idx0; i < idx1; i++) {
                work[A.row_idx[i]]++;
            }
            idx0 = idx1;
        }

        // construct col_idx in the transposed matrix
        colsum(C,work);

        // fill in the row indexes
        idx0 = A.col_idx[0];
        for (int j = 1; j <= A.numCols; j++) {
            int col = j-1;
            int idx1 = A.col_idx[j];
            for (int i = idx0; i < idx1; i++) {
                int row = A.row_idx[i];
                int index = work[row]++;
                C.row_idx[index] = col;
                C.data[index] = A.data[i];
            }
            idx0 = idx1;
        }
    }

    /**
     * Given the histogram of columns compute the col_idx for the matrix.  Then overwrite histogram with
     * those values.
     * @param A A matrix
     * @param histogram histogram of column values in the sparse matrix
     */
    public static void colsum( SMatrixCC_F64 A, int histogram[] ) {
        A.col_idx[0] = 0;
        int index = 0;
        for (int i = 1; i <= A.numCols; i++) {
            A.col_idx[i] = index += histogram[i-1];
        }
        System.arraycopy(A.col_idx,0,histogram,0,A.numCols);
    }

    /**
     * Performs matrix addition:<br>
     * C = &alpha;A + &beta;B
     *
     * @param alpha scalar value multiplied against A
     * @param A Matrix
     * @param beta scalar value multiplied against B
     * @param B Matrix
     * @param C Output matrix.
     * @param x (Optional) Work space.  null or as long as A.rows.
     */
    public static void add(double alpha , SMatrixCC_F64 A , double beta , SMatrixCC_F64 B , SMatrixCC_F64 C ,
                           double x[] )
    {
        x = checkDeclareRows(A, x);

        C.length = 0;

        for (int col = 0; col < A.numCols; col++) {
            C.col_idx[col] = C.length;

            // construct the table now so that the row order will not need to be sorted later on
            int idxA0 = A.col_idx[col], idxA1 = A.col_idx[col+1];
            int idxB0 = B.col_idx[col], idxB1 = B.col_idx[col+1];
            int indexA = idxA0, indexB = idxB0;

            while( indexA < idxA1 || indexB < idxB1 ) {
                int row;
                if( indexA < idxA1 && indexB < idxB1 ) {
                    int rowA = A.row_idx[indexA];
                    int rowB = B.row_idx[indexB];

                    if( rowA < rowB ) {
                        row = rowA; indexA++;
                    } else if( rowA > rowB ) {
                        row = rowB; indexB++;
                    } else {
                        row = rowA; indexA++; indexB++;
                    }
                } else if( indexA < idxA1 ) {
                    row = A.row_idx[indexA++];
                } else {
                    row = B.row_idx[indexB++];
                }

                if( C.length >= C.row_idx.length ) {
                    C.growMaxLength(C.length*2+1,true);
                }

                C.row_idx[C.length] = row;
                C.col_idx[col+1] = ++C.length;
                x[row] = 0;
            }


            // Add A
            for (int j = idxA0; j < idxA1; j++) {
                int row = A.row_idx[j];
                x[row] += A.data[j]*alpha;
            }

            // Add B
            for (int j = idxB0; j < idxB1; j++) {
                int row = B.row_idx[j];
                x[row] += B.data[j]*beta;
            }

            // take the values in the dense vector 'x' and put them into 'C'
            int idxC0 = C.col_idx[col];
            int idxC1 = C.col_idx[col+1];

            for (int i = idxC0; i < idxC1; i++) {
                C.data[i] = x[C.row_idx[i]];
            }
        }
    }

    public static int[] checkDeclareRows(SMatrixCC_F64 A, int[] w, boolean fillZeros) {
        if( w == null )
            w = new int[A.numRows];
        else if( w.length < A.numRows )
            throw new IllegalArgumentException("w needs to at least be as long as A.numRows");
        else if( fillZeros )
            Arrays.fill(w,0,A.numRows,0);
        return w;
    }

    public static double[] checkDeclareRows(SMatrixCC_F64 A, double[] x) {
        if( x == null )
            x = new double[A.numRows];
        else if( x.length < A.numRows )
            throw new IllegalArgumentException("x needs to at least be as long as A.numRows");
        return x;
    }
}