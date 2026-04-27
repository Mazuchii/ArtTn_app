# Score matching candidats/poste

Le script [python/candidate_matching.py](/C:/Users/Lenovo/Desktop/ProjetJava/python/candidate_matching.py) calcule un score de compatibilité entre une offre et une liste de candidats.

Il compare:

- les mots-clés de l'offre
- le texte du CV
- la lettre de motivation

Format d'entrée JSON:

```json
{
  "offer": {
    "titre": "Développeur Python Backend",
    "description": "API REST, SQL, Git, tests..."
  },
  "candidates": [
    {
      "candidat_id": "candidat_001",
      "cv_text": "Texte du CV",
      "lettre_motivation": "Texte de motivation"
    }
  ]
}
```

Commande:

```powershell
python python/candidate_matching.py python/sample_matching_input.json --pretty
```

Commande texte lisible:

```powershell
python python/candidate_matching.py python/sample_matching_input.json --text
```

Notes:

- `cv_text` est recommandé.
- Si `cv_text` est vide, le script essaie de lire `cv_url` uniquement pour les fichiers `.txt` et `.md`.
- Les PDF ne sont pas extraits automatiquement dans cette version.
- La sortie retourne `ranking` trié et `best_match`.
