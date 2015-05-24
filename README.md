# Genera

Ever imagine a world, where your persistence-layer is completely abstracted away from the core logic of your application?
Genera is the realization of this ideal.  With Genera, it will be possible to design an application that is completely isolated
from the underlying database, to the point that one could not only transition from one RDBMS database to another (e.g. postgres
to Maria DB), but even from an relational database to a NoSQL datastore (e.g. Mongo, Couch or Cassandra): all without major changes
to your application.  The key is the utilization of all-purpose storage models (presently: key-value pairs and collections) and
defining constrants on how the data is used, to allow Genera to internally optimize, as appropriate, for the chosen underlying
data-store.

# Disclaimer

This is an early-stage open-source project.  It's architecture is still in flux, features are partially implemented
at each level of abstraction.  Presently, only postgres is being supported, but Cassandra will be the likely next Database
to be targeted.  But, there are number of changes that are pending (hence: the reason some of the code doesn't ... 
well, compile :-/)

# Where are we and what's next?

The current implementation has recently evolved from an initial proof-of-concept towards a more elegant design, 
that will allow one to choose the Database back-end by importing the appropriate implicit of the back-end to be used.
This will require recompilation, whenever the database changes, but code changes will be minimal (as a new import will be
selected, and an implicit Authorization object would be defined within the scope of usage).  Do not count on any particulars
of the current design to not face radical changes: in fact, it would be best to count on it.  But, there is a trajectory set 
for Genera, which can be summarized as follows:

  * Initial open-source "release" (DONE)
  * Complete the current iteration on the structure of the GeneraStore core components, and revise tests to exercise the functionality.
  * Revamp the DSL.  By using Shapeless, it should be possible to serialize/deserialize from case-classes and HLists to/from Genera "Things".
  * Consider expanding the GeneraStore data modeling feature set, and RDMS/postgres-specific optimizations (e.g. indexes, JSON data storage).
  * Investigate the possibility of introducing Pure-Functional persistence capabilities (i.e. where Genera Things are immutable). 
