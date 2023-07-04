# Client authentication means

Authentication means are credentials with which the Yolt API can talk to a [provider](provider.md) on behalf of a [client](client.md).
`site-management` does not manage or keep track of authentication means, but we do listen for changes to authentication means.

Based on messages about created/updated/deleted authentication means (that we receive over Kafka) we update a table called `client_enabled_provider`. 
This class represents the table: `nl.ing.lovebird.sitemanagement.site.clientsite.domain.ClientEnabledProviderV2`

That table contains a row for every combination of `{ client, provider, serviceType }` for which the Yolt API has authentication means.

###### Synonyms

Colloquially these names are used too:

- "*authentication means*"
- "*client configuration*", this is actually the 'preferred nomenclature'
