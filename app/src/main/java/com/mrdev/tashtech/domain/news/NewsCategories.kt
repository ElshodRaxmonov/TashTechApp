package com.mrdev.tashtech.domain.news

import com.mrdev.tashtech.domain.model.NewsCategory

object NewsCategories {
    private val rules: List<Pair<NewsCategory, Set<String>>> = listOf(
        NewsCategory.Robotics to setOf("robotics", "robot", "robots", "drone", "drones"),
        NewsCategory.Semiconductors to setOf("semiconductors", "semiconductor", "chips", "chip", "foundry"),
        NewsCategory.AI to setOf("ai", "artificial intelligence", "machine learning", "generative ai", "llm", "chatgpt", "openai", "ai agents"),
        NewsCategory.Security to setOf("security", "cybersecurity", "privacy", "hacking", "vulnerability"),
        NewsCategory.Space to setOf("space", "aerospace", "nasa", "spacex", "satellite", "rocket"),
        NewsCategory.Transportation to setOf("transportation", "cars", "car", "automotive", "electric vehicles", "ev", "evs", "autonomous vehicles", "trains", "mobility"),
        NewsCategory.Energy to setOf("energy", "climate", "climate tech", "battery", "batteries", "nuclear", "solar"),
        NewsCategory.Telecom to setOf("telecommunications", "telecom", "5g", "networking", "wireless"),
        NewsCategory.Hardware to setOf("hardware", "gadgets", "consumer electronics", "pcs", "devices"),
        NewsCategory.Software to setOf("software", "apps", "open source", "developers", "programming"),
        NewsCategory.Computing to setOf("computing", "computers", "quantum", "biz & it", "information technology", "cloud"),
        NewsCategory.Science to setOf("science", "biomedical", "health", "physics", "biotech & health"),
    )
    private val startupWords = setOf("startups", "venture", "fundraising", "enterprise")

    fun classify(rawCategories: List<String>, title: String): NewsCategory {
        val cats = rawCategories.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        rules.firstOrNull { (_, words) -> cats.any { it in words } }?.let { return it.first }
        val tokens = title.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.toSet()
        rules.firstOrNull { (_, words) -> tokens.any { it in words } }?.let { return it.first }
        if (cats.any { it in startupWords }) return NewsCategory.Startups
        return NewsCategory.Tech
    }
}