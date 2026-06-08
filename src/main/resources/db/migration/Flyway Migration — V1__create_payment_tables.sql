CREATE TABLE payments (
                          id                        VARCHAR(36)  PRIMARY KEY,
                          order_id                  VARCHAR(100) NOT NULL,
                          stripe_payment_intent_id  VARCHAR(100) UNIQUE,
                          user_id                   VARCHAR(36)  NOT NULL,
                          amount                    DECIMAL(19,4) NOT NULL,
                          currency                  CHAR(3)      NOT NULL,
                          status                    VARCHAR(20)  NOT NULL,
                          failure_code              VARCHAR(100),
                          failure_message           VARCHAR(500),
                          idempotency_key           VARCHAR(200) UNIQUE,
                          version                   BIGINT       DEFAULT 0,
                          created_at                TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                          updated_at                TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
                              ON UPDATE CURRENT_TIMESTAMP,
                          INDEX idx_order_id (order_id),
                          INDEX idx_user_id  (user_id)
);

CREATE TABLE audit_logs (
                            id               VARCHAR(36)   PRIMARY KEY,
                            action           VARCHAR(100)  NOT NULL,
                            entity_id        VARCHAR(36),
                            user_id          VARCHAR(36),
                            ip_address       VARCHAR(45),
                            details          TEXT,
                            correlation_id   VARCHAR(36),
                            created_at       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE idempotency_keys (
                                  key_value    VARCHAR(200)  PRIMARY KEY,
                                  response     TEXT          NOT NULL,
                                  created_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
                                  expires_at   TIMESTAMP     NOT NULL,
                                  INDEX idx_expires (expires_at)
);
