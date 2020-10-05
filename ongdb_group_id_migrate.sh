#!/bin/bash

#set -o xtrace

# <groupId>org.neo4j
for current_file in $(grep -l -r '<groupId>org\.neo4j<\/groupId>' .); do
  echo "Migrating: $current_file"
  sed -i 's/<groupId>org\.neo4j<\/groupId>/<groupId>org\.graphfoundation\.ongdb<\/groupId>/g' "$current_file"
done

# <groupId>org.neo4j.*
for sub_space in app community test licensing-proxy build; do
  for current_file in $(grep -l -r "<groupId>org\.neo4j\.$sub_space<\/groupId>" .); do
    echo "Migrating: $current_file"
    sed -i "s/<groupId>org\.neo4j\.$sub_space<\/groupId>/<groupId>org\.graphfoundation\.ongdb\.$sub_space<\/groupId>/g" "$current_file"
  done
done

# <artifactId>ongdb-
for current_file in $(grep -l -r '<artifactId>ongdb-' .); do
  echo "Migrating: $current_file"
  sed -i 's/<artifactId>ongdb-/<artifactId>ongdb-/g' "$current_file"
done

# <artifactId>neo4j-java-driver
for sub_space in ongdb-java-driver; do
  for current_file in $(grep -l -r '<artifactId>neo4j-java-driver' .); do
    echo "Migrating: $current_file"
    sed -i 's/<artifactId>neo4j-java-driver/<artifactId>neo4j-java-driver/g' "$current_file"
  done
done

# <name>ONgDB
for current_file in $(grep -l -r '<name>ONgDB' .); do
  echo "Migrating: $current_file"
  sed -i 's/<name>ONgDB/<name>ONgDB/g' "$current_file"
done

# <description>ONgDB
for current_file in $(grep -l -r '<description>ONgDB' .); do
  echo "Migrating: $current_file"
  sed -i 's/<description>ONgDB/<description>ONgDB/g' "$current_file"
done

git add .
git commit -am "Updating org.neo4j to org.graphfoundation.ongdb"

echo "End migration"
