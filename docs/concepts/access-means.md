# Access means

Conceptually, access means is a set of data that a [provider](provider.md) uses to prove to a [site](site.md) that we have the consent of a [user](user.md) to retrieve their data.
This is a bit of a lie when we consider scraping providers, read on to find out why.

We store access means in either one of two tables:

1. `access_means`: (`nl.ing.lovebird.sitemanagement.accessmeans.domain.AccessMeans`)
   - `userId`: the [user](user.md) to which the access means belong
   - `provider`: the [provider](provider.md) to which the access means are coupled
   - `accessMeans`: the access means itself

2. `user_site_access_means`: (`nl.ing.lovebird.sitemanagement.accessmeans.domain.UserSiteAccessMeans`)
   - `userId`: the [user](user.md) to which the access means belong
   - `userSiteId`: the [user-site](user-site.md) to which the access means belong
   - `accessMeans`: the access means itself

What is the difference?

The `access_means` are access means with which a [user](user.md) can _authenticate_ him or herself at a scraping [provider](provider.md).
In practice we abstract this away completely for the [user](user.md), users don't know that they have an 'account' at a scraper.
That scraping [provider](provider.md) stores a [user](user.md)'s login information for `[1..n]` different [site](site.md)s.

The `user_site_access_means` are in fact what was described above: a set of data with which a [provider](provider.md) can prove that a user has given consent.

## Properties

**created**

Contains the timestamp at which UserSiteAccessMeans were created.
This field is not a typical createdAt timestamp that is set exactly once and is left alone afterwards.
This field has functional meaning and therefore it *must* be set to 'now' whenever new accessmeans are retrieved from a site.
In other words: this field needs to be updated whenever the consent is created or updated.
We communicate this field to clients to give them an indication of how much longer a consent might be valid for at a site.


###### A note on the `accessMeans` field

We store the access means as an encrypted opaque blob.
The reason for this extra layer of encryption (at-rest) is that the data is sensitive.

###### Access means vs. Client authentication means

[access-means](access-means.md) and [client-authentication-means](client-authentication-means.md) are —although similarly named— different concepts:

- [client-authentication-means](client-authentication-means.md) are used by a [provider](provider.md) to _connect to a [site](site.md) on behalf of a [client](client.md)_.

- [access-means](access-means.md) are used by a [provider](provider.md) _to prove to a [site](site.md) that the [client](client.md) has consent to retrieve data belonging to the [user](user.md)_.
