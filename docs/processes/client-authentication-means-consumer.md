# Client authentication means consumer

The [client-authentication-means](../concepts/client-authentication-means.md) consumer is a Kafka listener that listens on topic: `yolt.kafka.topics.clientAuthenticationMeans.topic-name`.

Whenever a [client](../concepts/client.md) makes a change to their [client-authentication-means](../concepts/client-authentication-means.md), a message is posted to this topic by the system *(todo: by what part of the system?)*.

There are several message types, defined in `nl.ing.lovebird.sitemanagement.site.clientsite.ClientAuthenticationMeansMessageType`, we'll treat them separately:
- `CLIENT_AUTHENTICATION_MEANS_CREATED`
- `CLIENT_AUTHENTICATION_MEANS_UPDATED`
- `CLIENT_AUTHENTICATION_MEANS_DELETED`

# Created

If the message contains a `redirectUrlId`, we upsert a [client-redirect-url-enabled-provider](../concepts/client-redirect-url-enabled-provider.md).
Otherwise, we upsert a [client-enabled-provider](../concepts/client-enabled-provider.md).

# Updated

See [Created](#created)

# Deleted

If the message contains a `redirectUrlId`, we delete the relevant [client-redirect-url-enabled-provider](../concepts/client-redirect-url-enabled-provider.md).
Otherwise, we delete the relevant [client-enabled-provider](../concepts/client-enabled-provider.md).