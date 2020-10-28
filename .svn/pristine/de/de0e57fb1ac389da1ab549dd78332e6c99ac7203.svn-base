create or replace NONEDITIONABLE PROCEDURE "A_SP_POST_MINI_TRANSACTIONS" (
    IV_MSGTYPE       IN NVARCHAR2 ,
    IV_FIELD2        IN NVARCHAR2 ,
    IV_FIELD3        IN NVARCHAR2,
    IV_FIELD4        IN NUMBER DEFAULT 0,--AMOUNT
    IV_FIELD7        IN NVARCHAR2 DEFAULT '' ,
    IV_FIELD11       IN NVARCHAR2 ,--STAN
    IV_FIELD24       IN NVARCHAR2,
    IV_FIELD32       IN NVARCHAR2 ,
    IV_FIELD35       IN NVARCHAR2 DEFAULT '' ,
    IV_FIELD37       IN NVARCHAR2,
    IV_FIELD65       IN NVARCHAR2 DEFAULT NULL ,
    IV_FIELD68       IN NVARCHAR2,
    IV_FIELD90       IN NVARCHAR2 DEFAULT '' ,
    IV_FIELD100      IN NVARCHAR2,---SERVICE TN/TA
    IV_FIELD101      IN NVARCHAR2 DEFAULT '' ,
    IV_FIELD102      IN NVARCHAR2,
    IV_FIELD103      IN NVARCHAR2 DEFAULT '' ,
    IV_COMMISSION    IN NUMBER DEFAULT 0,
    IV_CUSTCURRENCY  IN NVARCHAR2 DEFAULT 'UGX' ,
    IV_TERMINALID    IN NVARCHAR2 DEFAULT '' ,
    IV_USERID        IN NVARCHAR2 DEFAULT NULL ,
    IV_TRNCODE       IN NVARCHAR2 DEFAULT NULL ,
    IV_EXTERNALREFNO IN NVARCHAR2 DEFAULT NULL, --E.G MPESA OR CBS
    C_1              IN OUT SYS_REFCURSOR)
AS
  V_WORKINGDATE DATE;
  V_FINANCIALYR NVARCHAR2(6);
  V_FINANCIALPRD NVARCHAR2(3);
  V_TRXREFNO NVARCHAR2(200) :='';
  V_PARENTREF NVARCHAR2(70):=IV_FIELD90;
  V_COB    NUMBER             :=0;
  V_AMOUNT NUMERIC(18,5)      :=IV_FIELD4;
  V_REVERSALRETURNMESSAGE NVARCHAR2(500) ;
  CV_1 SYS_REFCURSOR;
  V_LIMIT_RESPONSE_CODE NVARCHAR2(10);
  V_LIMIT_RESPONSE_DESC NVARCHAR2(100);
  V_G_C NVARCHAR2(2);
  V_DR_CR NVARCHAR2(2);
  V_VALIDATION NVARCHAR2(10);
  V_GCACCOUNTNO NVARCHAR2(20);
  V_serialno NUMBER (10,0)      :=0;-- :=TBTRANSACTIONS_ID.nextval;
  V_PAYMENT_NUMBER NVARCHAR2(10):='';
  V_FIELD41 NVARCHAR2(50)       :='';
  V_FIELD42 NVARCHAR2(50)       :='';
  V_LEAFGL_DR NVARCHAR2(50)     :='';
  V_LEAFGL_CR NVARCHAR2(50)     :='';
  V_TRXCODE NVARCHAR2(50)       :='7777'; --Differentiate Charges from Actual Transactions
  V_clearbal          NUMBER (18,5)      :=0;
  V_AVAILABLE_BALANCE NUMBER (18,5)      :=0;
  V_CHARGES_AMT       NUMBER (18,5)      :=0;
  V_COMMISSION_AMT       NUMBER (18,5)      :=0;
  V_EXCISE_DUTY_RATE  NUMBER (18,5)      :=0;
  V_EXCISE_DUTY_AMT   NUMBER (18,5)      :=0;
  V_balanceaccount    NVARCHAR2(100)      :='';
  V_available_actual NVARCHAR2(100)      :='';
  V_MINI_DATA VARCHAR2(4000)             :='';
  V_FIELD103 NVARCHAR2(50)       :=IV_FIELD103;

  V_CHECK_COMMISSION_ACCOUNT NUMBER := 0;
  V_AGENT_COMMISSION_ACCOUNT NVARCHAR2(50) := '';

  --- gemification

    V_LOYALITY_POINTS_ACCOUNT NVARCHAR2(50) :='';
    V_TOTAL_LOYALITY_POINTS NUMBER(18) :='';

  -- limit check
  V_LIMIT_CHECK NUMBER(5) :=0;

  -- BANK and eclectics accounts
  V_GET_ECLE_COMM_GL NVARCHAR2(50) :='';
  V_GET_BANK_COMM_GL NVARCHAR2(50) :='';
  V_COMM_SPLIT NUMBER(18) :=0;
  V_ECLE_COMM NUMBER(18) :=0;
  V_BANK_COMM NUMBER(18) :=0;

  --
BEGIN
--Take Note-- To make PostMini less bulky, Modularize, use Functions to do most things.
  SAVEPOINT V_SAFEPOINT;
  BEGIN
    SELECT WORKINGDATE,
      FINANCIALYEAR ,
      FINANCIALPERIOD,
      COB
    INTO V_WORKINGDATE,
      V_FINANCIALYR,
      V_FINANCIALPRD,
      V_COB
    FROM TB_DATE_SETTINGS
    WHERE ROWNUM = 1;
  EXCEPTION
  WHEN OTHERS THEN

    OPEN C_1 FOR SELECT ('57' ||'|' || 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR FETCHING WORKING DATE')DATAX FROM DUAL;
    ROLLBACK TO SAVEPOINT V_SAFEPOINT;
    RETURN;
  END;
  BEGIN
    IF IV_FIELD100 = 'FT_BULK_PAYMENT' THEN
      V_FIELD103:= FN_AUTO_REG_BULK_PAY(IV_FIELD35,IV_FIELD65);
    END IF;
     EXCEPTION
  WHEN OTHERS THEN
    OPEN C_1 FOR SELECT ('57' ||'|' || 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR IN AUTO-REGISTRATION FOR BULK PAYMENT')DATAX FROM DUAL;
    ROLLBACK TO SAVEPOINT V_SAFEPOINT;
    RETURN;
  END;
  BEGIN
    V_VALIDATION :=FN_VALIDATE_CUSTOMER(IV_FIELD2,IV_FIELD24,IV_FIELD102,V_FIELD103);
       IF V_VALIDATION = 'NOTOK' THEN
            ROLLBACK TO SAVEPOINT V_SAFEPOINT;
            OPEN C_1 FOR SELECT ( '11' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR IN ACCOUNT-PHONENUMBER VALIDATION')DATAX FROM DUAL;
            RETURN;
        END IF;
  EXCEPTION
  WHEN OTHERS THEN

    OPEN C_1 FOR SELECT ('57' ||'|' || 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR VALIDATING CUSTOMER PHONE_NUMBER')DATAX FROM DUAL;
    ROLLBACK TO SAVEPOINT V_SAFEPOINT;
    RETURN;
  END;


  --Get Leaf GL
  BEGIN
    IF FN_IS_WALLET(IV_FIELD102) > 0 THEN
      --CA-Customer Account, Agent Account, Merchant Account, Group Account
      SELECT LEAF_GL
      INTO V_LEAFGL_DR
      FROM TB_PRODUCTS
      WHERE PRODUCT_CODE = SUBSTR(IV_FIELD102,1,3)
      AND ROWNUM         =1;
    END IF;
    IF FN_IS_WALLET(V_FIELD103) > 0 THEN
      --CA-Customer Account, Agent Account, Merchant Account, Group Account
      SELECT LEAF_GL
      INTO V_LEAFGL_CR
      FROM TB_PRODUCTS
      WHERE PRODUCT_CODE = SUBSTR(V_FIELD103,1,3)
      AND ROWNUM         =1;
    END IF;
  EXCEPTION
  WHEN OTHERS THEN

    OPEN C_1 FOR SELECT ('57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR GETTING LEAF GL')DATAX FROM DUAL;
    ROLLBACK TO SAVEPOINT V_SAFEPOINT;
    RETURN;
  END;
  --1) VALIDATE DR/CR ACCOUNTS/TXN
  BEGIN
    IF IV_FIELD102 = V_FIELD103 THEN
      OPEN C_1 FOR SELECT ('15' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR DEBIT TO SAME CREDIT ACCOUNT')DATAX FROM DUAL;
      ROLLBACK TO SAVEPOINT V_SAFEPOINT;
      RETURN;
    END IF;
  EXCEPTION
  WHEN OTHERS THEN
  RAISE;
    OPEN C_1 FOR SELECT ('57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR COMPARING DEBIT AND CREDIT ACCOUNTS')DATAX FROM DUAL;
    ROLLBACK TO SAVEPOINT V_SAFEPOINT;
    RETURN;
  END;
  BEGIN
    IF FN_VALIDATE_TXN_TYPE(IV_FIELD3,IV_FIELD100,IV_FIELD4,IV_FIELD24) < 1 THEN
      OPEN C_1 FOR SELECT ('57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'INVALID FIELD3 OR FIELD100 OR TRX_MATRIX CONFIG NOT DONE')DATAX FROM DUAL;
      ROLLBACK TO SAVEPOINT V_SAFEPOINT;
      RETURN;
    END IF;
  EXCEPTION
  WHEN OTHERS THEN
    OPEN C_1 FOR SELECT ('57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR VALIDATING FIELD3 AND FIELD100')DATAX FROM DUAL;
    ROLLBACK TO SAVEPOINT V_SAFEPOINT;
    RETURN;
  END;
  --2) GET TRX REFERENCE
  BEGIN
    V_TRXREFNO  := FN_GET_REFERENCE(IV_FIELD32);
    V_PARENTREF := IV_FIELD11 ||V_TRXREFNO||IV_FIELD37; --FIELD90 AKA PARENTREF
  EXCEPTION
  WHEN OTHERS THEN
    RAISE;
    OPEN C_1 FOR SELECT ('57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR GENERATING TRANSACTION REFERENCE')DATAX FROM DUAL;
    ROLLBACK TO SAVEPOINT V_SAFEPOINT;
    RETURN;
  END;
  --3) CHECK IF ITS A REVERSAL REQUEST AND PROCESS
  IF IV_MSGTYPE = '0420' THEN
    BEGIN
      V_REVERSALRETURNMESSAGE:= '00';
      V_REVERSALRETURNMESSAGE := FN_MINI_REV(IV_FIELD32, IV_FIELD37, IV_FIELD90);
      NULL;
      OPEN C_1 FOR SELECT (V_REVERSALRETURNMESSAGE)DATAX FROM DUAL;
      RETURN;
    EXCEPTION
    WHEN OTHERS THEN
      OPEN C_1 FOR SELECT ('57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR REVERSING TRANSACTION')DATAX FROM DUAL;
      ROLLBACK TO SAVEPOINT V_SAFEPOINT;
      RETURN;
    END;
  END IF;
  --4) CHECK TRANS LIMITS
  BEGIN
    A_SP_CHECK_TXN_LIMIT_GLOBAL(IV_FIELD2,IV_FIELD4, IV_FIELD24, IV_FIELD32,IV_FIELD100, trim(IV_FIELD102), IV_CUSTCURRENCY,CV_1);
    LOOP
      FETCH CV_1 INTO V_LIMIT_RESPONSE_CODE,V_LIMIT_RESPONSE_DESC;
      EXIT
    WHEN CV_1%NOTFOUND;
      IF V_LIMIT_RESPONSE_CODE NOT IN ('00') THEN
        OPEN C_1 FOR SELECT ( '16' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || V_LIMIT_RESPONSE_DESC)DATAX FROM DUAL;
        RETURN;
      END IF;
      --DBMS_OUTPUT.PUT_LINE(V_LIMIT_RESPONSE_CODE || ' ' || V_LIMIT_RESPONSE_DESC);
    END LOOP;
    CLOSE CV_1;
  EXCEPTION
  WHEN OTHERS THEN
    RAISE;
    OPEN C_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR CHECKING LIMITS')DATAX FROM DUAL;
    ROLLBACK TO SAVEPOINT V_SAFEPOINT;
    RETURN;
  END;
  --4) Check Balances
  BEGIN
    IF FN_IS_WALLET(IV_FIELD102) > 0 THEN
      V_AVAILABLE_BALANCE       := FN_GETBALANCE_AVAILABLE('C', V_COB, trim(IV_FIELD102));
     DBMS_OUTPUT.put_line(V_AVAILABLE_BALANCE);
      V_CHARGES_AMT         := FN_GET_CHARGES(iv_Field3,iv_Field4,iv_Field32,iv_Field100,iv_Field102,V_FIELD103);
      DBMS_OUTPUT.put_line(V_CHARGES_AMT);
      IF V_AVAILABLE_BALANCE < V_AMOUNT + V_CHARGES_AMT AND FN_IS_WALLET(IV_FIELD102) > 0 THEN
        OPEN C_1 FOR SELECT ( '51' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'INSUFFICIENT FUNDS')DATAX FROM DUAL;
        RETURN;
      END IF;
    END IF;
  EXCEPTION
  WHEN OTHERS THEN
    RAISE;
    OPEN C_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR CHECKING CUSTOMER BALANCES')DATAX FROM DUAL;
    ROLLBACK TO SAVEPOINT V_SAFEPOINT;
    RETURN;
  END;
  --5) POST THE TRANSACTION
  BEGIN
    DECLARE
      CURSOR CUR_TRXMATRIX
      IS
        SELECT ACCOUNTNO,
          DRCR ,
          FVR ,
          MF ,
          G_C
        FROM TB_TRANSACTION_MATRIX
        WHERE
          --CHANNEL = IV_FIELD32 AND
          PROCODE    = IV_FIELD3
        AND FIELD100 = IV_FIELD100
        AND FIELD24  =IV_FIELD24
        AND V_AMOUNT > 0
        ORDER BY DRCR DESC;
      V_TRXMATRIXDATA CUR_TRXMATRIX%ROWTYPE;
    BEGIN
      -- OPEN THE CURSOR AND INITIALIZE THE ACTIVE SET
      OPEN CUR_TRXMATRIX;
      -- RETRIEVE THE FIRST ROW, TO SET UP FOR THE WHILE LOOP
      FETCH CUR_TRXMATRIX
      INTO V_TRXMATRIXDATA;
      -- CONTINUE LOOPING WHILE THERE ARE MORE ROWS TO FETCH
      WHILE CUR_TRXMATRIX%FOUND
      LOOP
        V_DR_CR := V_TRXMATRIXDATA.DRCR;
        V_G_C   := V_TRXMATRIXDATA.G_C;
        --  V_GCACCOUNTNO          := V_TRXMATRIXDATA.ACCOUNTNO ;x
        V_gcaccountno          :=FN_GET_GL_ACCOUNT(IV_FIELD100,IV_FIELD3);
        IF LENGTH(V_GCACCOUNTNO)<1 AND V_TRXMATRIXDATA.G_C = 'G' THEN--VALIDATE GL IS CONFIGURED
          BEGIN

            ROLLBACK TO SAVEPOINT V_SAFEPOINT;
            OPEN C_1 FOR SELECT ( '11' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'GL ACCOUNT NOT FOUND(GL MAPPING ERROR)')DATAX FROM DUAL;
            RETURN;
          EXCEPTION
          WHEN OTHERS THEN
          DBMS_OUTPUT.PUT_LINE('ERROR ON GL ACCOUNT...');
            ROLLBACK TO SAVEPOINT V_SAFEPOINT;
            OPEN C_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR READING GL ACCOUNT')DATAX FROM DUAL;
            RETURN;
          END;
        END IF;
        IF V_TRXMATRIXDATA.G_C   = 'C' THEN
          IF V_TRXMATRIXDATA.DRCR= 'D' THEN
            V_VALIDATION        :=FN_VALIDATE_ACCOUNT(IV_FIELD102,'D');
            V_GCACCOUNTNO       :=(IV_FIELD102);
          ELSE
            V_VALIDATION  :=FN_VALIDATE_ACCOUNT(V_FIELD103,'C');
            V_GCACCOUNTNO :=(V_FIELD103);
          END IF;
          IF V_VALIDATION = 'NOTOK' THEN
            ROLLBACK TO SAVEPOINT V_SAFEPOINT;
            OPEN C_1 FOR SELECT ( '11' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR IN ACCOUNT VALIDATION')DATAX FROM DUAL;
            RETURN;
          END IF;
        END IF;
        --5A) UPDATE BALANCES
        IF V_COB = 0 THEN --COB CHECK DONT UPDATE BALANCES DURING COB
          ----------------------------------------------
          BEGIN
            --UPDATE CUSTOMERS BALANCES
            IF V_TRXMATRIXDATA.G_C    = 'C' THEN
              IF V_TRXMATRIXDATA.DRCR = 'D' THEN --DEBIT CUSTOMER
                UPDATE TB_ACCOUNT
                SET ACTUAL_BAL           = NVL(ACTUAL_BAL,0)    - (V_AMOUNT ),
                  AVAILABLE_BAL          = NVL(AVAILABLE_BAL,0) - (V_AMOUNT ),
                  TODAY_DR               = NVL(TODAY_DR, 0)     + (V_AMOUNT ),
                  LAST_TRANSACTION_DATE  = V_WORKINGDATE
                WHERE ACCOUNT_NO         = V_GCACCOUNTNO
                AND ROWNUM               =1;        --PUT rownum to avoid FTS (Full table Scan)

                  V_LIMIT_CHECK := FN_CHECK_ACCOUNT_LIMIT(V_GCACCOUNTNO, 'D');

                  IF V_LIMIT_CHECK < 0 THEN
                     ROLLBACK TO SAVEPOINT V_SAFEPOINT;
                     OPEN C_1 FOR SELECT ( '20' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' ||
                      'Transaction amount higher than the balance')DATAX FROM DUAL;
                    RETURN;
                  END IF;
              ELSIF V_TRXMATRIXDATA.DRCR = 'C' THEN --CREDIT CUSTOMER
                UPDATE TB_ACCOUNT
                SET ACTUAL_BAL          = NVL(ACTUAL_BAL,0)    + (V_AMOUNT ),
                  AVAILABLE_BAL         = NVL(AVAILABLE_BAL,0) +(V_AMOUNT ),
                  TODAY_CR              = NVL(TODAY_CR, 0)     + (V_AMOUNT ),
                  LAST_TRANSACTION_DATE = V_WORKINGDATE
                WHERE ACCOUNT_NO        = V_GCACCOUNTNO
                AND ROWNUM              =1;
              END IF;

              V_LIMIT_CHECK := FN_CHECK_ACCOUNT_LIMIT(V_GCACCOUNTNO, 'D'); --incase of a higher balance than what is configured
              -- it will block all DR
            END IF;--end of CUSTbalances Update
          EXCEPTION
          WHEN OTHERS THEN
            ROLLBACK TO SAVEPOINT V_SAFEPOINT;
            OPEN C_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'ERROR Updating Customer Balances')DATAX FROM DUAL;
            RETURN;
          END;
          -----------------------------------------------------------
          BEGIN
            --UPDATE GL BALANCES---
            IF V_TRXMATRIXDATA.G_C    = 'G' THEN
              IF V_TRXMATRIXDATA.DRCR = 'D' THEN --DEBIT CUSTOMER

                UPDATE TB_GL_BALANCE
                SET Gl_Balance           = NVL(Gl_Balance,0)  - (V_AMOUNT ),
                  Dr_Bal_Lcy             = NVL(Dr_Bal_Lcy, 0) + (V_AMOUNT )
                WHERE GL_CODE            = V_GCACCOUNTNO
                AND ROWNUM               =1;
              ELSIF V_TRXMATRIXDATA.DRCR = 'C' THEN --CREDIT CUSTOMER
                UPDATE TB_GL_BALANCE
                SET Gl_Balance = NVL(Gl_Balance,0)  + (V_AMOUNT ),
                  Cr_Bal_Lcy   = NVL(Cr_Bal_Lcy, 0) + (V_AMOUNT )
                WHERE GL_CODE  = V_GCACCOUNTNO
                AND ROWNUM     =1;
                --Update Leaf GL Balances--
                IF FN_IS_WALLET(V_GCACCOUNTNO) > 0 THEN --UPDATE LEAF GLS BLANCES, DEBIT LEAF
                  UPDATE TB_GL_BALANCE
                  SET Gl_Balance                  = NVL(Gl_Balance,0)  - (V_AMOUNT ),
                    Dr_Bal_Lcy                    = NVL(Dr_Bal_Lcy, 0) + (V_AMOUNT )
                  WHERE GL_CODE                   = V_LEAFGL_DR
                  AND ROWNUM                      =1;
                ELSIF FN_IS_WALLET(V_GCACCOUNTNO) > 0 THEN --UPDATE LEAF GLS BLANCES, CREDIT LEAF
                  UPDATE TB_GL_BALANCE
                  SET Gl_Balance = NVL(Gl_Balance,0)  + (V_AMOUNT ),
                    Cr_Bal_Lcy   = NVL(Cr_Bal_Lcy, 0) + (V_AMOUNT )
                  WHERE GL_CODE  = V_LEAFGL_CR
                  AND ROWNUM     =1;
                END IF;
              END IF;
              --Update leaf GLs
            END IF; --end of gLbalances Update
          EXCEPTION
          WHEN OTHERS THEN
            RAISE;
            DBMS_OUTPUT.PUT_LINE('ERROR ON GL BALANCE...');
            ROLLBACK TO SAVEPOINT V_SAFEPOINT;
            OPEN C_1 FOR SELECT ( '57' || 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Updating GL Balances')DATAX FROM DUAL;
            RETURN;
          END;
        END IF;
        ---Insert into tb_transactions--
        -------------------------------------
        BEGIN
          A_SP_INSERT_TRANSACTIONS( IV_MSGTYPE, IV_FIELD32, IV_FIELD37, V_serialno + 1, IV_FIELD68, V_GCACCOUNTNO, IV_CUSTCURRENCY, V_DR_CR, IV_FIELD3, IV_FIELD100, iv_Field11, V_AMOUNT, V_Workingdate, V_Financialyr, V_Financialprd, IV_USERID, v_G_C, V_parentref, IV_FIELD65, V_TRXCODE, V_FIELD41, V_FIELD42, IV_FIELD24,V_COB,V_TRXREFNO);
          V_serialno:=V_serialno                                                   +1;
        EXCEPTION
        WHEN OTHERS THEN
          RAISE;
          OPEN c_1 FOR SELECT ('57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Logging Transaction (Principle) - COB flag-'||V_COB ) DATAX FROM DUAL;
          ROLLBACK TO SAVEPOINT v_SafePoint;
          RETURN;
        END;
        FETCH CUR_TRXMATRIX INTO V_TRXMATRIXDATA;
      END LOOP;
      -- FREE RESOURCES USED BY THE CURSOR
      CLOSE CUR_TRXMATRIX;
    EXCEPTION
    WHEN OTHERS THEN
      --RAISE;
      ROLLBACK TO SAVEPOINT V_SAFEPOINT;
      OPEN C_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Updating GL Balances')DATAX FROM DUAL;
      RETURN;
    END;
  END;

  --6) Post Charges
  BEGIN
    IF V_CHARGES_AMT > 0 THEN
      V_TRXCODE     := '9999';--charges Code
      BEGIN
        V_GCACCOUNTNO     :=FN_GET_GL_AC_CHARGES(IV_FIELD100,IV_FIELD3);
        V_EXCISE_DUTY_RATE:= FN_GETGENERAL_PARAMS('EXCISE_DUTY');

        --get gl for eclectics charges and for the bank
        -- we need to split the charge commission based on what is set in the general params


      EXCEPTION
      WHEN OTHERS THEN
        --RAISE;
        ROLLBACK TO SAVEPOINT V_SAFEPOINT;
        OPEN C_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Fetching Charges GL Account')DATAX FROM DUAL;
        RETURN;
      END;
      --6a)Debit Customer
      IF V_COB = 0 THEN
        UPDATE TB_ACCOUNT
        SET ACTUAL_BAL          = NVL(ACTUAL_BAL,0)    - (V_CHARGES_AMT ),
          AVAILABLE_BAL         = NVL(AVAILABLE_BAL,0) - (V_CHARGES_AMT ),
          TODAY_DR              = NVL(TODAY_DR, 0)     + (V_CHARGES_AMT ),
          LAST_TRANSACTION_DATE = V_WORKINGDATE
        WHERE ACCOUNT_NO        = IV_FIELD102
        AND ROWNUM              =1;
        --6b) Credit Wallet Income GL
        UPDATE TB_GL_BALANCE
        SET Gl_Balance = NVL(Gl_Balance,0)  + (V_CHARGES_AMT ),
          Cr_Bal_Lcy   = NVL(Cr_Bal_Lcy, 0) + (V_CHARGES_AMT )
        WHERE GL_CODE  = V_GCACCOUNTNO
        AND ROWNUM     =1;
        --Update Leaf GL Balances--
        IF FN_IS_WALLET(IV_FIELD102) > 0 THEN --UPDATE LEAF GLS BLANCES, DEBIT LEAF
          UPDATE TB_GL_BALANCE
          SET Gl_Balance = NVL(Gl_Balance,0)  - (V_CHARGES_AMT ),
            Dr_Bal_Lcy   = NVL(Dr_Bal_Lcy, 0) + (V_CHARGES_AMT )
          WHERE GL_CODE  = V_LEAFGL_DR
          AND ROWNUM     =1;
        END IF;
      END IF;
      --6c) Post  Charges tbtransactions
      BEGIN
        --Post Charges DR Leg --Customer
        A_SP_INSERT_TRANSACTIONS(IV_MSGTYPE, IV_FIELD32, IV_FIELD37, V_serialno + 1, IV_FIELD68 || ' -Charges', IV_FIELD102, IV_CUSTCURRENCY, 'D', IV_FIELD3, IV_FIELD100, iv_Field11, V_CHARGES_AMT, V_Workingdate, V_Financialyr, V_Financialprd, IV_USERID, 'C',V_parentref, IV_FIELD65, V_TRXCODE, V_FIELD41, V_FIELD42, IV_FIELD24,V_COB,V_TRXREFNO);
        V_serialno:=V_serialno                                                  +1;
        --Post Charges CR leg Income GL
        A_SP_INSERT_TRANSACTIONS(IV_MSGTYPE, IV_FIELD32, IV_FIELD37, V_serialno + 1, IV_FIELD68 || ' -Charges', V_GCACCOUNTNO, IV_CUSTCURRENCY, 'C', IV_FIELD3, IV_FIELD100, iv_Field11, V_CHARGES_AMT, V_Workingdate, V_Financialyr, V_Financialprd, IV_USERID, 'G', V_parentref, IV_FIELD65, V_TRXCODE, V_FIELD41, V_FIELD42, IV_FIELD24,V_COB,V_TRXREFNO);
        V_serialno:=V_serialno                                                  +1;
      EXCEPTION
      WHEN OTHERS THEN
        RAISE;
        OPEN c_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Logging Transaction Charges' ) DATAX FROM DUAL;
        ROLLBACK TO SAVEPOINT v_SafePoint;
        RETURN;
      END;
    END IF;
  END;
  ----------------

  ----6b)---------------------------------------credit the commission accounts for both the bank and eclectics accounts
--
--  BEGIN
--
--    BEGIN
--          V_GET_ECLE_COMM_GL := FN_GET_GL_ACCOUNT('ECLECTICS_COMMISSION','000000'); -- GET ECL AC
--          V_GET_ECLE_COMM_GL := FN_GET_GL_ACCOUNT('BANK_INCOME_ACCOUNT','000000'); -- GET BANK AC
--        EXCEPTION
--        WHEN OTHERS THEN
--          OPEN c_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error fetching commission accounts' ) DATAX FROM DUAL;
--          ROLLBACK TO SAVEPOINT v_SafePoint;
--    END;
--
--    IF V_CHARGES_AMT   > 0 THEN -- THIS CHARGE WILL BE DEBTED FROM THE CHARGE GL BEFORE BEING POSTED TO THE 2 ACCOUNTS
--
--      V_COMM_SPLIT := FN_GETGENERAL_PARAMS('ECLECTICS_COMMISSION'); -- GET THE COMMISSION SPLIT SET IN THE ACCOUNT
--
--      -- split the commission into the two amounts
--      V_ECLE_COMM := V_CHARGES_AMT * V_COMM_SPLIT / 100;
--      V_BANK_COMM := V_CHARGES_AMT - V_ECLE_COMM ;
--
--
--        BEGIN
--      -- PERFORM TRANSACTION FOR ECLE
--      -- debit income gl
--          UPDATE TB_GL_BALANCE
--        SET Gl_Balance = NVL(Gl_Balance,0)  - (V_ECLE_COMM ),
--          Dr_Bal_Lcy   = NVL(Dr_Bal_Lcy, 0) + (V_ECLE_COMM )
--        WHERE GL_CODE  = V_GCACCOUNTNO
--        AND ROWNUM     =1;
--      -- credit eclectics income account
--        UPDATE TB_GL_BALANCE
--        SET Gl_Balance = NVL(Gl_Balance,0)  + (V_ECLE_COMM ),
--          Cr_Bal_Lcy   = NVL(Cr_Bal_Lcy, 0) + (V_ECLE_COMM )
--        WHERE GL_CODE  = V_GET_ECLE_COMM_GL
--        AND ROWNUM     =1;
--
--      -- LOG THE TRANSACTION
--                  --Post Charges DR Leg --Customer
--           A_SP_INSERT_TRANSACTIONS(IV_MSGTYPE, IV_FIELD32, IV_FIELD37, V_serialno + 1, IV_FIELD68 || ' -ECL COMM', V_GCACCOUNTNO, IV_CUSTCURRENCY, 'D', IV_FIELD3, IV_FIELD100, iv_Field11, V_CHARGES_AMT, V_Workingdate, V_Financialyr, V_Financialprd, IV_USERID, 'C',V_parentref, IV_FIELD65, V_TRXCODE, V_FIELD41, V_FIELD42, IV_FIELD24,V_COB,V_TRXREFNO);
--           V_serialno:=V_serialno                                                  +1;
--           --Post Charges CR leg Income GL
--           A_SP_INSERT_TRANSACTIONS(IV_MSGTYPE, IV_FIELD32, IV_FIELD37, V_serialno + 1, IV_FIELD68 || ' -ECL COMM', V_GET_ECLE_COMM_GL, IV_CUSTCURRENCY, 'C', IV_FIELD3, IV_FIELD100, iv_Field11, V_CHARGES_AMT, V_Workingdate, V_Financialyr, V_Financialprd, IV_USERID, 'G', V_parentref, IV_FIELD65, V_TRXCODE, V_FIELD41, V_FIELD42, IV_FIELD24,V_COB,V_TRXREFNO);
--          V_serialno:=V_serialno                                                  +1;
--          EXCEPTION
--          WHEN OTHERS THEN
--          RAISE;
--          OPEN c_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Logging Charge Commission-ECL' ) DATAX FROM DUAL;
--          ROLLBACK TO SAVEPOINT v_SafePoint;
--          RETURN;
--       END;
--
--      -- PERFORM TRANSACTION FOR BANK COMM
--         BEGIN
--              -- debit income gl
--          UPDATE TB_GL_BALANCE
--        SET Gl_Balance = NVL(Gl_Balance,0)  - (V_BANK_COMM ),
--          Dr_Bal_Lcy   = NVL(Dr_Bal_Lcy, 0) + (V_BANK_COMM )
--        WHERE GL_CODE  = V_GCACCOUNTNO
--        AND ROWNUM     =1;
--      -- credit eclectics income account
--        UPDATE TB_GL_BALANCE
--        SET Gl_Balance = NVL(Gl_Balance,0)  + (V_BANK_COMM ),
--          Cr_Bal_Lcy   = NVL(Cr_Bal_Lcy, 0) + (V_BANK_COMM )
--        WHERE GL_CODE  = V_GET_ECLE_COMM_GL
--        AND ROWNUM     =1;
--
--      -- LOG THE TRANSACTION
--                  --Post Charges DR Leg --Customer
--           A_SP_INSERT_TRANSACTIONS(IV_MSGTYPE, IV_FIELD32, IV_FIELD37, V_serialno + 1, IV_FIELD68 || ' -BANK COMM', V_GCACCOUNTNO, IV_CUSTCURRENCY, 'D', IV_FIELD3, IV_FIELD100, iv_Field11, V_CHARGES_AMT, V_Workingdate, V_Financialyr, V_Financialprd, IV_USERID, 'C',V_parentref, IV_FIELD65, V_TRXCODE, V_FIELD41, V_FIELD42, IV_FIELD24,V_COB,V_TRXREFNO);
--           V_serialno:=V_serialno                                                  +1;
--           --Post Charges CR leg Income GL
--           A_SP_INSERT_TRANSACTIONS(IV_MSGTYPE, IV_FIELD32, IV_FIELD37, V_serialno + 1, IV_FIELD68 || ' -BANK COMM', V_GET_ECLE_COMM_GL, IV_CUSTCURRENCY, 'C', IV_FIELD3, IV_FIELD100, iv_Field11, V_CHARGES_AMT, V_Workingdate, V_Financialyr, V_Financialprd, IV_USERID, 'G', V_parentref, IV_FIELD65, V_TRXCODE, V_FIELD41, V_FIELD42, IV_FIELD24,V_COB,V_TRXREFNO);
--          V_serialno:=V_serialno                                                  +1;
--          EXCEPTION
--          WHEN OTHERS THEN
--          RAISE;
--          OPEN c_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Logging Charge Commission-BANK' ) DATAX FROM DUAL;
--          ROLLBACK TO SAVEPOINT v_SafePoint;
--          RETURN;
--       END;
--
--
--    END IF;
--  END;
--




  --7) Excise Duty
  BEGIN
    V_EXCISE_DUTY_AMT   :=V_EXCISE_DUTY_RATE/100 *V_CHARGES_AMT;
    V_GCACCOUNTNO       := FN_GET_GL_ACCOUNT('EXCISE_DUTY','999999');
    IF V_EXCISE_DUTY_AMT > 0 THEN
      V_TRXCODE         := '8888';--ExciseDuty Code
      IF V_COB           = 0 THEN
        UPDATE TB_ACCOUNT
        SET ACTUAL_BAL          = NVL(ACTUAL_BAL,0)    - (V_EXCISE_DUTY_AMT ),
          AVAILABLE_BAL         = NVL(AVAILABLE_BAL,0) - (V_EXCISE_DUTY_AMT ),
          TODAY_DR              = NVL(TODAY_DR, 0)     + (V_EXCISE_DUTY_AMT ),
          LAST_TRANSACTION_DATE = V_WORKINGDATE
        WHERE ACCOUNT_NO        = IV_FIELD102
        AND ROWNUM              =1;
        --6b) Credit Wallet Income GL
        UPDATE TB_GL_BALANCE
        SET Gl_Balance = NVL(Gl_Balance,0)  + (V_EXCISE_DUTY_AMT ),
          Cr_Bal_Lcy   = NVL(Cr_Bal_Lcy, 0) + (V_EXCISE_DUTY_AMT )
        WHERE GL_CODE  = V_GCACCOUNTNO
        AND ROWNUM     =1;
        --Update Leaf GL Balances--
        IF FN_IS_WALLET(IV_FIELD102) > 0 THEN --UPDATE LEAF GLS BLANCES, DEBIT LEAF
          UPDATE TB_GL_BALANCE
          SET Gl_Balance = NVL(Gl_Balance,0)  - (V_EXCISE_DUTY_AMT ),
            Dr_Bal_Lcy   = NVL(Dr_Bal_Lcy, 0) + (V_EXCISE_DUTY_AMT)
          WHERE GL_CODE  = V_LEAFGL_DR
          AND ROWNUM     =1;
        END IF;
      END IF;
      BEGIN
        --Post Excise Duty DR Leg --Customer
        A_SP_INSERT_TRANSACTIONS(IV_MSGTYPE, IV_FIELD32, IV_FIELD37, V_serialno + 1, IV_FIELD68 || ' -ExciseDuty', IV_FIELD102, IV_CUSTCURRENCY, 'D', IV_FIELD3, IV_FIELD100, iv_Field11, V_EXCISE_DUTY_AMT, V_Workingdate, V_Financialyr, V_Financialprd, IV_USERID, 'C', V_parentref, IV_FIELD65, V_TRXCODE, V_FIELD41, V_FIELD42, IV_FIELD24,V_COB,V_TRXREFNO);
        V_serialno:=V_serialno                                                  +1;
        --Post Excise Duty CR leg Income GL
        A_SP_INSERT_TRANSACTIONS(IV_MSGTYPE, IV_FIELD32, IV_FIELD37, V_serialno + 1, IV_FIELD68 || ' -ExciseDuty', V_GCACCOUNTNO, IV_CUSTCURRENCY, 'C', IV_FIELD3, IV_FIELD100, iv_Field11, V_EXCISE_DUTY_AMT, V_Workingdate, V_Financialyr, V_Financialprd, IV_USERID, 'G', V_parentref, IV_FIELD65, V_TRXCODE, V_FIELD41, V_FIELD42, IV_FIELD24,V_COB,V_TRXREFNO);
        V_serialno:=V_serialno                                                  +1;
      EXCEPTION
      WHEN OTHERS THEN
        OPEN c_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Logging Transaction ExciseDuty' ) DATAX FROM DUAL;
        ROLLBACK TO SAVEPOINT v_SafePoint;
        RETURN;
      END;
    END IF;
  END;
  -- 8) Post Commission for agent

   BEGIN
   -- CHECK IF FLOAT ACCOUNT IS A WALLET ACCOUNT

   V_CHECK_COMMISSION_ACCOUNT := FN_CHECK_COMMISSION_AC(IV_FIELD35);

   IF V_CHECK_COMMISSION_ACCOUNT = 0 THEN

    V_AGENT_COMMISSION_ACCOUNT := FN_GET_COMM_ACCOUNT(IV_FIELD35);


      V_COMMISSION_AMT:=FN_GET_COMMISSIONS(IV_FIELD3,IV_FIELD4,IV_FIELD32,IV_FIELD100,IV_FIELD102,IV_FIELD103);
    IF V_CHARGES_AMT > 0 AND V_COMMISSION_AMT > 0  THEN
      V_TRXCODE     := '9999';--charges Code
      BEGIN
         V_GCACCOUNTNO       := FN_GET_GL_ACCOUNT('COMMISSION','999998'); --Expense GL
      EXCEPTION
      WHEN OTHERS THEN
        --RAISE;
        ROLLBACK TO SAVEPOINT V_SAFEPOINT;
        OPEN C_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Fetching Charges GL Account')DATAX FROM DUAL;
        RETURN;
      END;

      IF V_COB = 0 THEN
      --6a)Debit CoMMISSION gl
          UPDATE TB_GL_BALANCE
          SET Gl_Balance = NVL(Gl_Balance,0)  - (V_COMMISSION_AMT ),
            Dr_Bal_Lcy   = NVL(Dr_Bal_Lcy, 0) + (V_COMMISSION_AMT )
          WHERE GL_CODE  = V_GCACCOUNTNO
          AND ROWNUM     =1;

       --6b) Credit Agent Wallet Account

        UPDATE TB_ACCOUNT
        SET ACTUAL_BAL          = NVL(ACTUAL_BAL,0)    + (V_COMMISSION_AMT),
          AVAILABLE_BAL         = NVL(AVAILABLE_BAL,0) + (V_COMMISSION_AMT),
          TODAY_CR              = NVL(TODAY_CR, 0)     + (V_COMMISSION_AMT),
          LAST_TRANSACTION_DATE = V_WORKINGDATE
        WHERE ACCOUNT_NO        = V_AGENT_COMMISSION_ACCOUNT -- ENTER COMMISSION ACCOUNT
        AND ROWNUM              =1;

        --Update Leaf GL Balances--
        IF FN_IS_WALLET(V_AGENT_COMMISSION_ACCOUNT) > 0 THEN --UPDATE LEAF GLS BLANCES, DEBIT LEAF
          UPDATE TB_GL_BALANCE
          SET Gl_Balance = NVL(Gl_Balance,0)  + (V_COMMISSION_AMT ),
            Cr_Bal_Lcy   = NVL(Cr_Bal_Lcy, 0) + (V_COMMISSION_AMT)
          WHERE GL_CODE  = V_LEAFGL_CR
          AND ROWNUM     =1;
        END IF;
      END IF;
      --6c) Post  Commissions tbtransactions
      BEGIN
        --Post Commissions DR Leg --Customer
        A_SP_INSERT_TRANSACTIONS(IV_MSGTYPE, IV_FIELD32, IV_FIELD37, V_serialno + 1, IV_FIELD68 || ' -Commissions', V_GCACCOUNTNO, IV_CUSTCURRENCY, 'D', IV_FIELD3, IV_FIELD100, iv_Field11, V_COMMISSION_AMT, V_Workingdate, V_Financialyr, V_Financialprd, IV_USERID, 'G',V_parentref, IV_FIELD65, V_TRXCODE, V_FIELD41, V_FIELD42, IV_FIELD24,V_COB,V_TRXREFNO);
        V_serialno:=V_serialno                                                  +1;
        --Post Commissions CR leg Income GL
        A_SP_INSERT_TRANSACTIONS(IV_MSGTYPE, IV_FIELD32, IV_FIELD37, V_serialno + 1, IV_FIELD68 || ' -Commissions', IV_FIELD103, IV_CUSTCURRENCY, 'C', IV_FIELD3, IV_FIELD100, iv_Field11, V_COMMISSION_AMT, V_Workingdate, V_Financialyr, V_Financialprd, IV_USERID, 'C', V_parentref, IV_FIELD65, V_TRXCODE, V_FIELD41, V_FIELD42, IV_FIELD24,V_COB,V_TRXREFNO);
        V_serialno:=V_serialno                                                  +1;
      EXCEPTION
      WHEN OTHERS THEN
        RAISE;
        OPEN c_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Logging Transaction Charges' ) DATAX FROM DUAL;
        ROLLBACK TO SAVEPOINT v_SafePoint;
        RETURN;
      END;
    END IF;

   END IF;

  END;


--- end post commission

  --9) process loyality points for the account-----------------------------------------------------------------------------------------

  -- check if the transaction can earn points
  V_TOTAL_LOYALITY_POINTS := FN_GET_LOYALITY_POINTS(IV_FIELD3, IV_FIELD100, IV_FIELD4);

  IF V_TOTAL_LOYALITY_POINTS > 0 THEN

      -- fetch loyality points for field102
      -- get loyality points account
      V_LOYALITY_POINTS_ACCOUNT := FN_CHECK_LOYALITY_AC(IV_FIELD2);

       IF V_LOYALITY_POINTS_ACCOUNT <> '0' THEN

           -- fetch gl for loyality points
           BEGIN
             V_GCACCOUNTNO := FN_GET_GL_ACCOUNT('FT_LOYALTY_POINTS','400000' );

             -- debit loyality points GL
                UPDATE TB_GL_BALANCE
                SET Gl_Balance = NVL(Gl_Balance,0)  - (V_TOTAL_LOYALITY_POINTS ),
                Dr_Bal_Lcy   = NVL(Dr_Bal_Lcy, 0) + (V_TOTAL_LOYALITY_POINTS )
                WHERE GL_CODE  = V_GCACCOUNTNO
                AND ROWNUM     =1;

            -- credit loyality points GL
                  UPDATE TB_ACCOUNT
                  SET ACTUAL_BAL          = NVL(ACTUAL_BAL,0)    + (V_TOTAL_LOYALITY_POINTS),
                  AVAILABLE_BAL         = NVL(AVAILABLE_BAL,0) + (V_TOTAL_LOYALITY_POINTS),
                  TODAY_CR              = NVL(TODAY_CR, 0)     + (V_TOTAL_LOYALITY_POINTS),
                  LAST_TRANSACTION_DATE = V_WORKINGDATE
                  WHERE ACCOUNT_NO        = V_LOYALITY_POINTS_ACCOUNT -- ENTER COMMISSION ACCOUNT
                  AND ROWNUM              =1;

                --Update Leaf GL Balances--
                  IF FN_IS_WALLET(V_GCACCOUNTNO) > 0 THEN --UPDATE LEAF GLS BLANCES, DEBIT LEAF
                    UPDATE TB_GL_BALANCE
                    SET Gl_Balance                  = NVL(Gl_Balance,0)  - (V_AMOUNT ),
                      Dr_Bal_Lcy                    = NVL(Dr_Bal_Lcy, 0) + (V_AMOUNT )
                    WHERE GL_CODE                   = V_LEAFGL_DR
                    AND ROWNUM                      =1;
                  END IF;

                    --6c) Post  loyality points tbtransactions
                  BEGIN
                    --Post Commissions DR Leg --Customer
                    A_SP_INSERT_TRANSACTIONS(IV_MSGTYPE, IV_FIELD32, IV_FIELD37, V_serialno + 1, IV_FIELD68 || ' -Points', V_GCACCOUNTNO, 'PNT', 'D', IV_FIELD3, IV_FIELD100, iv_Field11, V_COMMISSION_AMT, V_Workingdate, V_Financialyr, V_Financialprd, IV_USERID, 'G',V_parentref, IV_FIELD65, V_TRXCODE, V_FIELD41, V_FIELD42, IV_FIELD24,V_COB,V_TRXREFNO);
                    V_serialno:=V_serialno                                                  +1;
                    --Post Commissions CR leg Income GL
                    A_SP_INSERT_TRANSACTIONS(IV_MSGTYPE, IV_FIELD32, IV_FIELD37, V_serialno + 1, IV_FIELD68 || ' -Points', V_LOYALITY_POINTS_ACCOUNT, 'PNT', 'C', IV_FIELD3, IV_FIELD100, iv_Field11, V_COMMISSION_AMT, V_Workingdate, V_Financialyr, V_Financialprd, IV_USERID, 'C', V_parentref, IV_FIELD65, V_TRXCODE, V_FIELD41, V_FIELD42, IV_FIELD24,V_COB,V_TRXREFNO);
                    V_serialno:=V_serialno                                                  +1;
                  EXCEPTION
                  WHEN OTHERS THEN
                   RAISE;
                    OPEN c_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Logging Loyality points' ) DATAX FROM DUAL;
                    ROLLBACK TO SAVEPOINT v_SafePoint;
                    RETURN;
                  END;
             EXCEPTION
             WHEN OTHERS THEN
             RAISE;
                 OPEN c_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Fetching loyality Gl account' ) DATAX FROM DUAL;
                  ROLLBACK TO SAVEPOINT v_SafePoint;
                RETURN;
           END;

       ELSE
          OPEN c_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Fetching loyality points account' ) DATAX FROM DUAL;
          ROLLBACK TO SAVEPOINT v_SafePoint;
          RETURN;
       END IF;


      --debit loyality gl

      -- credit loyality accounts
  END IF;





  ---------------------------------------end loyality points -----------------------------------------------


  IF SUBSTR(IV_FIELD24,0,1)    = 'M' THEN
    V_balanceaccount          := iv_Field102;
  ELSIF SUBSTR(IV_FIELD24,0,2) = 'BM' THEN
    V_balanceaccount          := V_FIELD103;
  END IF;
  BEGIN
    V_available_actual :=FN_GETBALANCE('C',V_COB,V_balanceaccount);
  EXCEPTION
  WHEN OTHERS THEN
    OPEN c_1 FOR SELECT ( '57' ||'|'|| 0 || '|' || 0 || '|' || 0 || '|' || 0 || '|' || 'Error Fetching Account Balance' ) DATAX FROM DUAL;
    ROLLBACK TO SAVEPOINT v_SafePoint;
    RETURN;
  END;
  IF IV_FIELD3 IN ('380000') THEN
    V_MINI_DATA:= FN_GET_MINI(iv_Field102);
    OPEN c_1 FOR SELECT ('00'||'|'||V_available_actual || '|' || V_CHARGES_AMT ||'|' || V_EXCISE_DUTY_AMT || '|' || 'Successful')DATAX, NVL(V_MINI_DATA,'') AS MINI_DATA FROM DUAL;
  ELSE
    OPEN c_1 FOR SELECT ('00'||'|'||V_available_actual || '|' || V_CHARGES_AMT ||'|' || V_EXCISE_DUTY_AMT || '|' || 'Successful')DATAX FROM DUAL;
  END IF;
  COMMIT;
  RETURN;
END;