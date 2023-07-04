# How do [providers](../concepts/provider.md), [sites](../concepts/site.md), and financial institutions relate?

A [provider](../concepts/provider.md) provides data for `[1..n]` [sites](../concepts/site.md).
A [site](../concepts/site.md) gets its data from precisely 1 [provider](../concepts/provider.md).

So far, so good.

However —and this is where it gets awkward— a *financial institution* might be represented by `[1..3]` separate [sites](../concepts/site.md).

In an ideal world, one would have _a single_ site that corresponds to a financial institution.
As you well know we do not live in an ideal world.


## Problem description

It might be the case that we have an existing scraping provider (call it `scraper`) for a site and many users happily using it.
The financial institution then exposes an API and we create a direct connection (let's call it `direct`) to that same institution.

Usually the following is the case:

- `direct` supports only a strict subset of the account types that `scraper` supports
- there are many users connected to the site via the `scraper`

What we want to achieve is this: move existing users off of `scraper` to `direct` for the _subset_ of account types that `direct` supports, and keep the other account types live via `scraper`.


## What could have been

What we could have done is simply alter the list of account types on the `scraper` site and create the `direct` site.
For an unknown reason —now long lost to the winds of time— this option was not chosen.

## What is instead

We need to set up two new sites for a total of _three_.
An example is probably the most instructive:

- `scraper`: the original scraper site that's been around for a while and that everyone uses
  ```
  Site[
    groupingBy="HSBC",
    connectionType="SCRAPER",
    whiteListedAccountTypes=["CURRENT_ACCOUNTS", "CREDIT_ACCOUNTS", "SAVINGS_ACCOUNTS"]
  ]
  ```
- `direct`: the new direct connection that supports a subset of account types
  ```
  Site[
    groupingBy="HSBC",
    connectionType="DIRECT_CONNECTION",
    whiteListedAccountTypes=["CURRENT_ACCOUNTS"]
  ]
  ```
- `scraper'`: a new scraper site that users will migrate to with the account types not supported by the direct connection
  ```
  Site[
    groupingBy="HSBC",
    connectionType="SCRAPER",
    whiteListedAccountTypes=["CREDIT_ACCOUNTS", "SAVINGS_ACCOUNTS"]
  ]
  ```

## Existing users connected to `scraper`

These users will need to migrate their [user-sites](../concepts/user-site.md) to `scraper'` and `direct` respectively.
This migration is triggered by performing the following actions:

- **todo** which actions?


## New users

New users must not connect to `scraper` but instead to `scraper'`, we prevent new users from connecting to `scraper'` as follows:

- **todo** how is this done? `newConnectionAvailable` has been removed


## History

This is a reconstruction for context, it might help explain the current way of working.
I'll keep it short.

### Single scraper

Yolt started out with _one_ 'provider' that made it possible to connect to several banks, or as we like to call them: [sites](../concepts/site.md).
The whole concept of a 'provider' did not exist at that time.
There were however multiple [site](../concepts/site.md)s, all of which were retrieved 'through' the single scraping provider.

### Multiple scrapers

Another scraping provider was added and the concept of a [provider](../concepts/provider.md) was introduced.
A [site](../concepts/site.md) would now have a reference to the associated [provider](../concepts/provider.md).

## Direct connections

A 'direct connection' was added, that is a provider that was built in-house.
Direct connections are characterized by a [site](../concepts/site.md) that is linked to a single [provider](../concepts/provider.md).

