DROP ALL objects;

-- BEGIN TABLE CATALOGUE
CREATE TABLE CATALOGUE
(
   CATALOGUE_ID           INTEGER      NOT NULL,
   CATALOGUE_NAME         VARCHAR(20)  NOT NULL,
   DESCRIPTION            VARCHAR(50)  NOT NULL,
   READ_ONLY              TINYINT      NOT NULL,
   CATALOGUE_ROOT_NODE_ID INTEGER      NOT NULL,
   CREATED                TIMESTAMP    NOT NULL,
   LAST_MODIFIED          TIMESTAMP,
   EXTERNAL_ID            VARCHAR(50)
);
ALTER TABLE CATALOGUE
   ADD CONSTRAINT PK_CAT PRIMARY KEY (CATALOGUE_ID);


COMMIT;
-- END TABLE CATALOGUE

-- BEGIN TABLE CATALOGUE_NODE
CREATE TABLE CATALOGUE_NODE
(
   CATALOGUE_NODE_ID INTEGER       NOT NULL,
   PARENT_NODE_ID    INTEGER,
   PRODUCT_ID        INTEGER,
   NODE_NAME         VARCHAR(100),
   EXTERNAL_ID       VARCHAR(50),
   SORT_ORDER        INTEGER       NOT NULL,
   CREATED           TIMESTAMP     NOT NULL,
   LAST_MODIFIED     TIMESTAMP,
   DELETED           TINYINT       NOT NULL
);
ALTER TABLE CATALOGUE_NODE
   ADD CONSTRAINT PK_CN PRIMARY KEY (CATALOGUE_NODE_ID);

COMMIT;
-- END TABLE CATALOGUE_NODE

-- BEGIN TABLE CATALOGUE_VISIBILITY
CREATE TABLE CATALOGUE_VISIBILITY
(
   CATALOGUE_ID INTEGER  NOT NULL,
   COMPANY_ID   INTEGER  NOT NULL
);
ALTER TABLE CATALOGUE_VISIBILITY
   ADD CONSTRAINT PK_CV PRIMARY KEY (CATALOGUE_ID,COMPANY_ID);

COMMIT;
-- END TABLE CATALOGUE_VISIBILITY

-- BEGIN TABLE LANGUAGE
CREATE TABLE LANGUAGE
(
   ISO_CODE  VARCHAR(2)   NOT NULL,
   LANG_NAME VARCHAR(25)  NOT NULL
);
ALTER TABLE LANGUAGE
   ADD CONSTRAINT PK_LANG PRIMARY KEY (ISO_CODE);

COMMIT;
-- END TABLE LANGUAGE

-- BEGIN TABLE LOCALISED_CATALOGUE_NODE
CREATE TABLE LOCALISED_CATALOGUE_NODE
(
   CATALOGUE_NODE_ID INTEGER       NOT NULL,
   LANGUAGE_CODE     VARCHAR(2)    NOT NULL,
   DISPLAY_NAME      VARCHAR(150)  NOT NULL,
   NODE_DESCRIPTION  CLOB
);
ALTER TABLE LOCALISED_CATALOGUE_NODE
   ADD CONSTRAINT PK_LCN PRIMARY KEY (CATALOGUE_NODE_ID,LANGUAGE_CODE);

COMMIT;
-- END TABLE LOCALISED_CATALOGUE_NODE

-- BEGIN TABLE PRODUCT
CREATE TABLE PRODUCT
(
   ID                             INTEGER       NOT NULL,
   DNS_PRODUCT_NUMBER             VARCHAR(30),
   MANUFACTURER_PRODUCT_NUMBER    VARCHAR(30)   NOT NULL,
   PRODUCT_NAME                   VARCHAR(100)  NOT NULL,
   LONG_DESCRIPTION               CLOB,
   MANUFACTURER_PRODUCT_CODE      VARCHAR(20),
   MANUFACTURER_PRODUCT_LINE_CODE VARCHAR(20),
   MANUFACTURER_PRODUCT_TYPE      VARCHAR(20),
   MANUFACTURER_PRODUCT_NAME      VARCHAR(100),
   MANUFACTURER_PRODUCT_GROUP     VARCHAR(20),
   MANUFACTURER_PRODUCT_ATTRIBUTE VARCHAR(20),
   START_DATE                     TIMESTAMP,
   END_DATE                       TIMESTAMP,
   BELONGS_TO_COMPANY_ID          INTEGER,
   LAST_MODIFIED                  TIMESTAMP     NOT NULL,
   VAT_CODE                       VARCHAR(10)
);
ALTER TABLE PRODUCT
   ADD CONSTRAINT PK_PRODUCT PRIMARY KEY (ID);

COMMIT;
-- END TABLE PRODUCT

-- BEGIN TABLE SHOP
CREATE TABLE SHOP
(
   ID                       INTEGER      NOT NULL,
   SHOP_NAME                VARCHAR(25)  NOT NULL,
   SHOP_COUNTRY_CODE        VARCHAR(2)   NOT NULL,
   DEFAULT_LANGUAGE_CODE    VARCHAR(2)   NOT NULL,
   PUBLICATION_CATALOGUE_ID INTEGER      NOT NULL,
   COMPANY_ID               INTEGER      NOT NULL,
   SOURCE_CATALOGUE_ID      INTEGER      NOT NULL,
   ORDER_NOTIFICATION_EMAIL VARCHAR(50)
);
ALTER TABLE SHOP
   ADD CONSTRAINT PK_SHOP PRIMARY KEY (ID);

COMMIT;
-- END TABLE SHOP

-- BEGIN FOREIGN KEYS --
ALTER TABLE CATALOGUE
  ADD CONSTRAINT CATALOGUE_ROOT_NODE_FK FOREIGN KEY (CATALOGUE_ROOT_NODE_ID)
  REFERENCES CATALOGUE_NODE (CATALOGUE_NODE_ID)
   ON UPDATE RESTRICT
   ON DELETE RESTRICT;

ALTER TABLE CATALOGUE_NODE
  ADD CONSTRAINT PARENT_CAT_NODE_FK FOREIGN KEY (PARENT_NODE_ID)
  REFERENCES CATALOGUE_NODE (CATALOGUE_NODE_ID)
   ON UPDATE RESTRICT
   ON DELETE RESTRICT;

ALTER TABLE CATALOGUE_NODE
  ADD CONSTRAINT PRODUCT_CATALOGUE_NODE_FK FOREIGN KEY (PRODUCT_ID)
  REFERENCES PRODUCT (ID)
   ON UPDATE RESTRICT
   ON DELETE RESTRICT;

ALTER TABLE CATALOGUE_VISIBILITY
  ADD CONSTRAINT PRODUCT_CATALOGUE_CATALOGUE_VISIBILITY_FK FOREIGN KEY (CATALOGUE_ID)
  REFERENCES CATALOGUE (CATALOGUE_ID)
   ON UPDATE RESTRICT
   ON DELETE RESTRICT;

ALTER TABLE LOCALISED_CATALOGUE_NODE
  ADD CONSTRAINT LOCALISED_NODE_FK FOREIGN KEY (CATALOGUE_NODE_ID)
  REFERENCES CATALOGUE_NODE (CATALOGUE_NODE_ID)
   ON UPDATE RESTRICT
   ON DELETE RESTRICT;

ALTER TABLE LOCALISED_CATALOGUE_NODE
  ADD CONSTRAINT LANGUAGE_LOCALISED_NODE_FK FOREIGN KEY (LANGUAGE_CODE)
  REFERENCES "LANGUAGE" (ISO_CODE)
   ON UPDATE RESTRICT
   ON DELETE RESTRICT;

ALTER TABLE SHOP
  ADD CONSTRAINT SHOP_CATALOGUE_FK FOREIGN KEY (PUBLICATION_CATALOGUE_ID)
  REFERENCES CATALOGUE (CATALOGUE_ID)
   ON UPDATE RESTRICT
   ON DELETE RESTRICT;

ALTER TABLE SHOP
  ADD CONSTRAINT SOURCE_CATALOGUE_FK FOREIGN KEY (SOURCE_CATALOGUE_ID)
  REFERENCES CATALOGUE (CATALOGUE_ID)
   ON UPDATE RESTRICT
   ON DELETE RESTRICT;

ALTER TABLE SHOP
  ADD CONSTRAINT LANGUAGE_SHOP_FK FOREIGN KEY (DEFAULT_LANGUAGE_CODE)
  REFERENCES "LANGUAGE" (ISO_CODE)
   ON UPDATE RESTRICT
   ON DELETE RESTRICT;

-- END FOREIGN KEYS --

