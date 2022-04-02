import gurobi.*;
import gurobi.GRB.IntParam;

import javax.swing.*;

public class Coppia16 {

    static final int ETICHETTE = 10;
    static final int FASCE_ORARIE= 6;

    public static void main(String[] args) {

        try{

            GRBEnv env = new GRBEnv("Coppia16.log");

            env.set(IntParam.Presolve, 0);
            env.set(IntParam.Method, 0);

            GRBModel model = new GRBModel(env);

            Integer[][] costi = {{1386, 1052, 1092, 1283, 912, 1216},
                                 {1276, 1282, 1327, 962, 1107, 934},
                                 {903, 1038, 1070, 1189, 1111, 1281},
                                 {1366, 1041, 1322, 1162, 1013, 1224},
                                 {1158, 1090, 1314, 942, 1043, 1374},
                                 {1320, 1075, 1105, 932, 1189, 1235},
                                 {908, 1066, 1252, 1334, 1000, 1011},
                                 {1238, 1021, 1236, 1370, 978, 904},
                                 {1050, 1259, 1322, 1080, 942, 1333},
                                 {1152, 921, 1339, 935, 1064, 1293}};

            Integer[][] spettatori = {{2703, 3249, 998, 1331, 709, 2869},
                                      {2242, 2038, 2026, 2077, 1524, 2557},
                                      {1243, 2016, 2761, 2890, 2691, 342},
                                      {609, 2292, 3364, 2296, 3140, 493},
                                      {2870, 2212, 1769, 2050, 1840, 1116},
                                      {2204, 3492, 1328, 2506, 2615, 3080},
                                      {869, 1711, 557, 3125, 2752, 890},
                                      {688, 3383, 2629, 3242, 3435, 3251},
                                      {2170, 1333, 3223, 1352, 3077, 2122},
                                      {1162, 1579, 947, 1070, 2895, 3137}};

            Integer[][] tempi_max =  {{2, 1, 2, 3, 2, 2},
                                      {1, 1, 2, 2, 2, 3},
                                      {2, 3, 3, 2, 1, 2},
                                      {2, 1, 1, 2, 2, 1},
                                      {1, 1, 2, 2, 2, 3},
                                      {3, 3, 2, 2, 3, 2},
                                      {2, 2, 2, 3, 2, 3},
                                      {2, 2, 1, 2, 3, 2},
                                      {3, 2, 2, 2, 1, 1},
                                      {1, 2, 2, 3, 2, 3} };

            Integer[] spesa_max_mittente = { 2810, 3441, 3205, 3443, 2968, 3127, 3258, 3334, 2933, 2826};
            double budget = 0;
            double omega = 1;
            double spettatori_min = 84486;

            //creazione variabili
            GRBVar[][] xij = new GRBVar[ETICHETTE][FASCE_ORARIE];
            for(int i=0; i<ETICHETTE; i++) {
                for (int j = 0; j < FASCE_ORARIE; j++) {
                    xij[i][j] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "xij_" + i + "_" + j);
                }
            }

            //creazione variabili di slack
            GRBVar[] y = new GRBVar[78];
            GRBLinExpr expr = new GRBLinExpr();

            //---------------------------------------F.O.----------------------------------------

            //funzione obiettivo con una variabile di sostegno
            y[0] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "y");
            expr.addTerm(1, y[0]);
            model.setObjective(expr);
            model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);


            //-------------------------------------VINCOLI---------------------------------------

            //aggiunta vincolo 1 : (equazione) <= y
            expr = new GRBLinExpr();
            for(int i=0; i<ETICHETTE; i++){
                for(int j=0; j<FASCE_ORARIE; j++){
                    if(j>FASCE_ORARIE/2){
                        expr.addTerm(spettatori[i][j], xij[i][j]);

                    }else {
                        expr.addTerm(-spettatori[i][j], xij[i][j]);
                    }
                }
            }
            model.addConstr(expr, GRB.LESS_EQUAL, y[0], "vincolo minore uguale");

            //aggiunta vincolo 2 : (equazione) >= y
            expr = new GRBLinExpr();
            for(int i=0; i<ETICHETTE; i++){
                for(int j=0; j<FASCE_ORARIE; j++){
                    if(j>FASCE_ORARIE/2){
                        expr.addTerm(spettatori[i][j], xij[i][j]);

                    }else {
                        expr.addTerm(-spettatori[i][j], xij[i][j]);
                    }
                }
            }
            model.addConstr(expr, GRB.GREATER_EQUAL, y[0], "vincolo maggiore uguale");


            //aggiunta vincoli: max minuti acquistabili in ciacuna fascia per ogni emittente
            for(int i=0; i<ETICHETTE; i++){
                for(int j=0; j<FASCE_ORARIE; j++) {
                    expr = new GRBLinExpr();
                    expr.addTerm(1, xij[i][j]);
                    model.addConstr(expr, GRB.LESS_EQUAL, tempi_max[i][j], "vincolo tempistica: i:" +i+ " j:" + j);
                }
            }

            //aggiunta vincoli: massima spesa per ogni emittente
            for(int i=0; i<ETICHETTE; i++) {

                expr = new GRBLinExpr();

                for(int j=0; j<FASCE_ORARIE; j++){
                    expr.addTerm(costi[i][j], xij[i][j]);
                }
                model.addConstr(expr, GRB.LESS_EQUAL, spesa_max_mittente[i], "massima spesa per l'emittente " +i);
            }

            //aggiunta vincoli: minima spesa per ciascun emittente, rispetto al budget tot
            //calcolo budget totale
            for(int i=0; i<ETICHETTE; i++){
                budget += spesa_max_mittente[i];
            }

            for(int j=0; j<FASCE_ORARIE; j++) {

                expr = new GRBLinExpr();

                for(int i=0; i<ETICHETTE; i++){
                    expr.addTerm(costi[i][j], xij[i][j]);
                }
                model.addConstr(expr, GRB.GREATER_EQUAL, omega/100*budget , "minima spesa per fascia oraria: " +j);
            }

            // minima copertura giornaliera di spettatori
            expr = new GRBLinExpr();
            for(int i=0; i<ETICHETTE; i++) {
                for(int j=0; j<FASCE_ORARIE; j++){
                    expr.addTerm(spettatori[i][j], xij[i][j]);
                }
            }
            model.addConstr(expr, GRB.GREATER_EQUAL, spettatori_min, "minimi spettatori");




            //ottimizzazione
            model.optimize();

            //stampa a video status del problema
            int status = model.get(GRB.IntAttr.Status);
            System.out.println("\n\n\nStato Ottimizzazione: "+ status);

            //stampa a video di tutte le variabili del modello
            for(GRBVar var : model.getVars())
            {
                System.out.println(var.get(GRB.StringAttr.VarName)+ ": "+ var.get(GRB.DoubleAttr.X));
            }

        }catch(GRBException e){
            System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
        }
    }
}
