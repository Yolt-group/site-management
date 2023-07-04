#!/usr/bin/env sh

set -x

# This variable can be changed to run against another team environment.  It is necessary but not sufficient, you will
# also have to create a application-teamX.yml profile.
team=team4
ns=ycs

# Configure kubectl to talk to $team
kubectl config use-context cluster0.$team >/dev/null
if [ $? -ne 0 ]; then
	>&2 echo failed to select cluster0.$team context, is this context available?
	exit 1
fi

# Create a folder to hold everything related to the team environment (secrets, trust store, ...)
mkdir -p target/$team
d=`pwd`/target/$team

# Grab latest vault token and store it in a file.
kubectl -n $ns get secrets --output=json | \
	jq -r '[.items[] | select(.metadata.name | contains("site-management-token-"))] | sort_by(.metadata.creationTimestamp) | first | .data.token' | \
   > $d/vaulttoken

# Copy some files from the team environment:
# - the vault secrets from /vault/secrets/*
# - the trust store from /etc/ssl/certs/java/cacerts
pod=$(kubectl -n $ns get pods | grep site-management | cut -f1 -d' ')
kubectl exec -n $ns $pod -- tar -c -C /vault/secrets -f - . | tar xf - -C $d
kubectl exec -n $ns $pod -- tar -c -C /etc/ssl/certs/java -f - cacerts | tar xf - -C $d

# Grab the jwks.
jwks=$(kubectl -n $ns get secret tokens-signature-jwks -o jsonpath="{.data.secret}" | base64 --decode)

# Grab the encryption key for access means.
encryption_key=$(kubectl -n $ns get secret site-management -o json | \
  jq -r '.data["encryption-key"]')

#
# Set up envvars.
#
# Directory containing vault secrets (YoltVaultAutoConfiguration)
export YOLT_VAULT_SECRETS_DIRECTORY=$d
# The token that site-management needs to authenticate at vault (YoltVaultAutoConfiguration)
export YOLT_VAULT_AUTH_SERVICE_ACCOUNT_TOKEN_FILE=$d/vaulttoken
# Kafka keystore and truststore (YoltKafkaAutoConfiguration)
export YOLT_VAULT_KAFKA_KEYSTORE_PATH=$d/kafka_key_store.jks
export YOLT_VAULT_KAFKA_TRUSTSTORE_PATH=$d/kafka_trust_store.jks
# Cassandra (YoltCassandraAutoConfiguration)
export YOLT_VAULT_CASSANDRA_VAULT_CREDS_FILE=$d/cassandra
# Postgres (YoltPostgreSQLAutoConfiguration)
export YOLT_VAULT_POSTGRESQL_VAULT_CREDS_FILE=$d/rds
# Public keys configured for ClientToken support (ClientTokenParserAutoConfiguration)
export SERVICE_TOKENS_SIGNATURE_JWKS="${jwks}"
# Access means encryption key.
export ACCESSMEANS_ENCRYPTIONKEY="${encryption_key}"

set +x

echo env configured for $team
