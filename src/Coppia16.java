import gurobi.*;
import gurobi.GRB.IntParam;

import static gurobi.GRB.IntAttr.VBasis;

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
                    xij[i][j] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "X " + i + "_" + j);
                }
            }

            //creazione variabili di slack
            GRBVar[] y = new GRBVar[79]; //2 + 60 + 10 + 6 + 1
            GRBLinExpr expr = new GRBLinExpr();
            for(int i=0; i<79; i++) {
                y[i] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "Y " + i);
            }

            //---------------------------------------F.O.----------------------------------------

            //funzione obiettivo con una variabile di sostegno
            GRBVar W = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "W");
            expr.addTerm(1, W);
            model.setObjective(expr);
            model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);


            //-------------------------------------VINCOLI---------------------------------------

            //aggiunta vincolo 1 : (a - b) +y0 = W
            expr = new GRBLinExpr();
            int contatore=0;//contatore che scorre il vettore delle variabili y
            for(int i=0; i<ETICHETTE; i++){
                for(int j=0; j<FASCE_ORARIE; j++){
                    if(j>FASCE_ORARIE/2){
                        expr.addTerm(-spettatori[i][j], xij[i][j]);
                    }else {
                        expr.addTerm(spettatori[i][j], xij[i][j]);
                    }
                }
            }
            expr.addTerm(1, y[contatore]);//y[0]
            contatore++;
            model.addConstr(expr, GRB.EQUAL, W, "vincolo minore uguale");

            //aggiunta vincolo 2 : (b - a) + y1 = W
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
            expr.addTerm(1, y[contatore]);//y[1]
            contatore++;
            model.addConstr(expr, GRB.EQUAL, W, "vincolo maggiore uguale");


            //aggiunta vincoli: max minuti acquistabili in ciacuna fascia per ogni emittente
            for(int i=0; i<ETICHETTE; i++){
                for(int j=0; j<FASCE_ORARIE; j++) {
                    expr = new GRBLinExpr();
                    expr.addTerm(1, xij[i][j]);
                    expr.addTerm(1, y[contatore]);//y[2]----y[61]
                    contatore++;
                    model.addConstr(expr, GRB.EQUAL, tempi_max[i][j], "vincolo tempistica: i:" +i+ " j:" + j);
                }
            }

            //aggiunta vincoli: massima spesa per ogni emittente
            for(int i=0; i<ETICHETTE; i++) {

                expr = new GRBLinExpr();

                for(int j=0; j<FASCE_ORARIE; j++){
                    expr.addTerm(costi[i][j], xij[i][j]);
                }
                expr.addTerm(1, y[contatore]);//y[62]------y[71]
                contatore++;
                model.addConstr(expr, GRB.EQUAL, spesa_max_mittente[i], "massima spesa per l'emittente " +i);
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
                expr.addTerm(-1, y[contatore]);//y[72]------y[77]
                contatore++;
                model.addConstr(expr, GRB.EQUAL, omega/100*budget , "minima spesa per fascia oraria: " +j);
            }

            // minima copertura giornaliera di spettatori
            expr = new GRBLinExpr();
            for(int i=0; i<ETICHETTE; i++) {
                for(int j=0; j<FASCE_ORARIE; j++){
                    expr.addTerm(spettatori[i][j], xij[i][j]);
                }
            }
            expr.addTerm(-1, y[contatore]);//y[78]
            model.addConstr(expr, GRB.EQUAL, spettatori_min, "minimi spettatori");




            //ottimizzazione
            model.optimize();

            //stampa a video status del problema
            //int status = model.get(GRB.IntAttr.Status);
            //System.out.println("\n\n\nStato Ottimizzazione: "+ status);

            //stampa a video di tutte le variabili del modello
/*
            for(GRBVar var : model.getVars())
            {
                System.out.println(var.get(GRB.StringAttr.VarName)+ ": "+ var.get(GRB.DoubleAttr.X));
            }

            for(GRBVar var : model.getVars())
            {
                System.out.println(var.get(GRB.StringAttr.VarName)+ "_costo coefficiente ridotto: "+ var.get(GRB.DoubleAttr.RC));
            }

            for(GRBConstr constr : model.getConstrs())
            {
                System.out.println(constr.get(GRB.StringAttr.ConstrName)+ ": "+ constr.get(GRB.DoubleAttr.Slack));
            }

*/
            //-----------------------------QUESITO 1--------------------------------------

            //calcolo copertura raggiunta totale
            double copertura_tot=0;
            for(int i=0; i<ETICHETTE; i++){
                for (int j=0; j<FASCE_ORARIE; j++){
                    copertura_tot += xij[i][j].get(GRB.DoubleAttr.X) * spettatori[i][j];
                }
            }
            //calcolo del tempo acquistato
            double tempo_acquistato = 0;
            for(int i=0; i<ETICHETTE; i++){
                for (int j=0; j<FASCE_ORARIE; j++){
                    tempo_acquistato += xij[i][j].get(GRB.DoubleAttr.X);
                }
            }
            //calcolo budget inutilizzato
            double budget_utilizzato = 0;
            for(int i=0; i<ETICHETTE; i++){
                for (int j=0; j<FASCE_ORARIE; j++){
                    budget_utilizzato += xij[i][j].get(GRB.DoubleAttr.X) * costi[i][j];
                }
            }

            // STAMPA A VIDEO
            System.out.println("GRUPPO <coppia 16>");
            System.out.println("Componenti: <Bresciani Simone> <Dagani Federico>\n");

            System.out.println("QUESITO I:");
            System.out.printf("funzione obiettivo = %.4f\n", model.get(GRB.DoubleAttr.ObjVal));
            System.out.printf("copertura raggiunta totale = %.4f spettatori\n", copertura_tot);
            System.out.printf("tempo acquistato = %.4f minuti\n", tempo_acquistato);
            System.out.printf("budget inutilizzato = %.4f\n", budget-budget_utilizzato);
            System.out.println("soluzione di base ottima:");
            for(GRBVar var : model.getVars()) {
                if(!var.get(GRB.StringAttr.VarName).equals("W"))
                    System.out.println(var.get(GRB.StringAttr.VarName) + " = " + var.get(GRB.DoubleAttr.X));
            }

            //-------------------------------QUESITO 2--------------------------------------

            System.out.println("\n\nQUESITO II:");
            //variabili in base
            System.out.printf("variabili in base: [");
            int c=0;
            for(GRBVar var : model.getVars())
                //controllo che il valore della variabile sia maggiore di zero (lontana dal vincolo) e il ccr = 0, dunque ho una variabile di base
                if(var.get(VBasis) == 0){
                    System.out.printf("1,");
                    c++;
                }else{
                    System.out.printf("0,");
                }
            System.out.printf("] e sono %d\n", c);
            //coeff CR
            System.out.printf("coefficienti di costo ridotto: [");
            for(GRBVar var : model.getVars())
                System.out.printf( "" + var.get(GRB.DoubleAttr.RC)+ ",");
            System.out.println("]");
            //soluzione ottima multipla
            boolean sol_ottima_multipla = false;
            for(GRBVar var: model.getVars())
                //se non sono in base e hanno i coefficenti di costo ridotto a zero
                if(var.get(VBasis) != 0 && var.get(GRB.DoubleAttr.RC) == 0) {
                    sol_ottima_multipla = true;
                    break;
                }
            System.out.printf("soluzione ottima multipla: %b\n", sol_ottima_multipla);
            //soluzione ottima degenere
            boolean sol_ottima_degenere = false;
            for (GRBVar var: model.getVars())
                //se sono in base e hanno i valori a zero
                if(var.get(VBasis)==0 && var.get(GRB.DoubleAttr.X) == 0){
                    sol_ottima_degenere = true;
                    break;
                }
            System.out.printf("soluzione ottima degenere: %b\n\n\n", sol_ottima_degenere);

            //-------------------------------QUESITO 3--------------------------------------
            System.out.printf("QUESITO III:\n");
            //PROCEDURA 1: Cottengo una sol ammissibile grazie al problema ausiliario
            //creo le variabili ausiliarie

            model.reset();
            model.remove(W);

            GRBVar[] h = new GRBVar[79]; //2 + 60 + 10 + 6 + 1
            expr = new GRBLinExpr();
            for(int i=0; i<79; i++) {
                h[i] = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "H " + i);
            }

            //---------------------------------------F.O. CON AUSILIARIE----------------------------------------
            //funzione obiettivo
            GRBVar K = model.addVar(0.0, GRB.INFINITY, 0.0, GRB.CONTINUOUS, "W");
            expr.addTerm(1, K);
            model.setObjective(expr);
            model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);

            //-------------------------------VINCOLI CON AUSILIARIE---------------------------------------

            //aggiunta vincolo 1 : (a - b) +y0 + h0= W
            expr = new GRBLinExpr();
            contatore = 0;
            for(int i=0; i<ETICHETTE; i++){
                for(int j=0; j<FASCE_ORARIE; j++){
                    if(j>FASCE_ORARIE/2){
                        expr.addTerm(-spettatori[i][j], xij[i][j]);
                    }else {
                        expr.addTerm(spettatori[i][j], xij[i][j]);
                    }
                }
            }
            expr.addTerm(1, y[contatore]);
            expr.addTerm(1, h[contatore]);
            contatore++;
            model.addConstr(expr, GRB.EQUAL, K, "vincolo minore uguale");

            //aggiunta vincolo 2 : (b - a) + y1 +h1= W
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
            expr.addTerm(1, y[contatore]);
            expr.addTerm(1, h[contatore]);
            contatore++;
            model.addConstr(expr, GRB.EQUAL, K, "vincolo maggiore uguale");


            //aggiunta vincoli: max minuti acquistabili in ciacuna fascia per ogni emittente
            for(int i=0; i<ETICHETTE; i++){
                for(int j=0; j<FASCE_ORARIE; j++) {
                    expr = new GRBLinExpr();
                    expr.addTerm(1, xij[i][j]);
                    expr.addTerm(1, y[contatore]);
                    expr.addTerm(1, h[contatore]);
                    contatore++;
                    model.addConstr(expr, GRB.EQUAL, tempi_max[i][j], "vincolo tempistica: i:" +i+ " j:" + j);
                }
            }

            //aggiunta vincoli: massima spesa per ogni emittente
            for(int i=0; i<ETICHETTE; i++) {

                expr = new GRBLinExpr();

                for(int j=0; j<FASCE_ORARIE; j++){
                    expr.addTerm(costi[i][j], xij[i][j]);
                }
                expr.addTerm(+1, y[contatore]);
                expr.addTerm(+1, h[contatore]);
                contatore++;
                model.addConstr(expr, GRB.EQUAL, spesa_max_mittente[i], "massima spesa per l'emittente " +i);
            }

            //aggiunta vincoli: minima spesa per ciascun emittente, rispetto al budget tot

            for(int j=0; j<FASCE_ORARIE; j++) {

                expr = new GRBLinExpr();

                for(int i=0; i<ETICHETTE; i++){
                    expr.addTerm(costi[i][j], xij[i][j]);
                }
                expr.addTerm(-1, y[contatore]);
                expr.addTerm(-1, h[contatore]);
                contatore++;
                model.addConstr(expr, GRB.EQUAL, omega/100*budget , "minima spesa per fascia oraria: " +j);
            }

            // minima copertura giornaliera di spettatori
            expr = new GRBLinExpr();
            for(int i=0; i<ETICHETTE; i++) {
                for(int j=0; j<FASCE_ORARIE; j++){
                    expr.addTerm(spettatori[i][j], xij[i][j]);
                }
            }
            expr.addTerm(-1, y[contatore]);
            expr.addTerm(-1, h[contatore]);
            model.addConstr(expr, GRB.EQUAL, spettatori_min, "minimi spettatori");

            model.optimize();

            boolean sol_amm_1 = true;
            //controllo che la funzione obiettivo del problema ausiliario sia zero
            if(model.get(GRB.DoubleAttr.ObjVal) == 0){
                //itero solo sulle variabili ausiliarie
                for(GRBVar var : h)
                    //se la variabile ausiliaria è in base con valore diverso da zero allora il mio problema non ha sol
                    if(var.get(GRB.IntAttr.VBasis) == 0 && var.get(GRB.DoubleAttr.X) != 0)
                        sol_amm_1 = false;
            }else{
                sol_amm_1 = false;
            }//guardo se sol_amm_1 è ancora vero, in caso comunico la sol di partenza, che non è l'ottimo
            if(sol_amm_1){
                System.out.println("soluzione di ammissibile non ottima:");
                for(GRBVar var : model.getVars()) {
                    if(!var.get(GRB.StringAttr.VarName).equals("K") && !var.get(GRB.StringAttr.VarName).equals("W") )
                        System.out.println(var.get(GRB.StringAttr.VarName) + " = " + var.get(GRB.DoubleAttr.X));
                }
            }else {
                System.out.println("Due fasi non applicabile");
            }
        }catch(GRBException e){
            System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
        }
    }
}
