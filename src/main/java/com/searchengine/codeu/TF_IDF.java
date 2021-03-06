package com.searchengine.codeu;


import org.jblas.DoubleMatrix;
import org.jblas.Geometry;

import java.util.ArrayList;
import java.util.Map;

import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Created by Vincent on 8/3/2016.
 */
public class TF_IDF {
    private JedisUniqueWordIndexer unique_index;
    private JedisIndex index;
    private Map<String, String> unique_words;

    public TF_IDF (JedisUniqueWordIndexer unique_index, JedisIndex index){
        this.index = index;
        this.unique_index = unique_index;
        this.unique_words = unique_index.getAll();
    }

    /**
     * Takes a list of query words and creates a vector
     * @param query_words words to create vector
     * @return vector representation of query
     */
    DoubleMatrix get_query_vector(ArrayList<String> query_words){
        int num_features = unique_words.size();
        double[] query_vector = new double[num_features];

        for (String word : query_words){
            String str_index = unique_words.get(word);
            if (str_index.equals(null)){
                continue;
            }
            Integer index = Integer.valueOf(str_index);
            query_vector[index] = query_vector[index] + 1;
        }
        return new DoubleMatrix(query_vector).transpose();
    }

    /**
     * Takes in a document termcounter and returns the vector representation
     * @param url_tc termcounter mapping from all terms to their freq for the document
     * @return vector version of document
     */
    DoubleMatrix get_document_vector(Map<String, String> url_tc){
        int num_features = unique_words.size();
        double[] query_vector = new double[num_features];

        for (String term : url_tc.keySet()){
            Integer index = Integer.valueOf(unique_words.get(term));
            if (index != null){
                query_vector[index] = Double.parseDouble(url_tc.get(term));
            }
        }
        return new DoubleMatrix(query_vector).transpose();
    }

    /**
     * Takes in the set of urls and returns matrix representation where each row is a document and each column is a
     * feature (1 unique word)
     * @param urls set of urls and their term counters
     * @return matrix representation of all the documents relevant to the search query
     */
    public DoubleMatrix get_document_matrix(ArrayList<String> query_words,
                                            ArrayList<String> urls){
        int num_features = unique_words.size();
        int num_rows = urls.size() + 1;
        DoubleMatrix doc_matrix = DoubleMatrix.zeros(num_rows, num_features);

        DoubleMatrix query_vector = get_query_vector(query_words);
        doc_matrix.putRow(0, query_vector);

        int row = 1;
        for (String document : urls){
            Map<String, String> doc = index.get_url_TC(document);
            DoubleMatrix row_vector = get_document_vector(doc);
            doc_matrix.putRow(row, row_vector);
            row = row + 1;
        }
        return doc_matrix;
    }

    DoubleMatrix matrix_mult(DoubleMatrix m1, DoubleMatrix m2){
        return m1.mmul(m2);
    }

    public DoubleMatrix normalize_rows(DoubleMatrix m1){
        Geometry geo = new Geometry();
        return geo.normalizeRows(m1);
    }

    DoubleMatrix get_diag_matrix(DoubleMatrix diag_vals){
        return DoubleMatrix.diag(diag_vals);
    }

    DoubleMatrix get_IDF_diag(double[] idf_vals){
        DoubleMatrix idf_matrix = new DoubleMatrix(idf_vals);
        return get_diag_matrix(idf_matrix);
    }

    public DoubleMatrix calculate_TFIDF(DoubleMatrix doc_vector){
        int total_docs = doc_vector.getRows();
        int total_features = doc_vector.getColumns();
        double[] tf_idf = new double[total_features];

        for(int i = 0; i < total_features; i++){
            DoubleMatrix col_vec = doc_vector.getColumn(i);
            int[] test = col_vec.findIndices();
            double doc_freq = test.length;
            double idf = log(total_docs / (doc_freq + 1));
            tf_idf[i] = idf;
        }

        return new DoubleMatrix(tf_idf).transpose();
    }

    public ArrayList<Double> cosine_similarity(int row_index, DoubleMatrix m1) {

        double cosin_similarity;
        int num_rows = m1.rows;
        int num_cols = m1.columns;

        ArrayList<Double> similar_rows = new ArrayList<Double>();

        for(int row = 0; row < num_rows; row++){
            double dot_product = 0.0, first_norm = 0.0, second_norm = 0.0;
            for (int column = 0; column < num_cols; column++) {
                dot_product += (m1.get(row_index, column) * m1.get(row, column));
                first_norm += pow(m1.get(row_index, column),2);
                second_norm += pow(m1.get(row, column), 2);
            }
            cosin_similarity = (dot_product / (sqrt(first_norm) * sqrt(second_norm)));
            similar_rows.add(row, cosin_similarity);
        }
        return similar_rows;
    }
}