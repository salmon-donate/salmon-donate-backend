#!/usr/bin/env groovy
import java.time.Instant

def create_new_migration(String name) {
    def sql_path = "./src/main/resources/db/sql/migrations/"

    if (name == null || name.isBlank()) {
        println("A filename can't be empty or blank")
        return
    }

    def timestamp = Instant.now().toEpochMilli()
    def filename = "${sql_path}${timestamp}_${name}.sql"

    def migration_file = new File(filename)
    try {
        migration_file.write("--liquibase formatted sql\n\n--changeset chekist32:${name}\n**Your code here**")
        println("Created ${filename}")
    } catch (IOException e) {
        println("An error occurred")
        e.printStackTrace()
    }
}

def name = args.length > 0
        ? args[0]
        : { print("Provide a filename: "); return System.in.newReader().readLine() }.call()

create_new_migration(name)