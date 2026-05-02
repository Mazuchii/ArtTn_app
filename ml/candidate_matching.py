import argparse
import json
import math
import re
import unicodedata
from collections import Counter
from pathlib import Path
from sklearn.ensemble import RandomForestClassifier
import pickle
import os


STOPWORDS = {
    "a", "au", "aux", "avec", "ce", "ces", "dans", "de", "des", "du", "elle",
    "en", "et", "eux", "il", "je", "la", "le", "les", "leur", "lui", "ma",
    "mais", "me", "meme", "mes", "moi", "mon", "ne", "nos", "notre", "nous",
    "on", "ou", "par", "pas", "pour", "qu", "que", "qui", "sa", "se", "ses",
    "son", "sur", "ta", "te", "tes", "toi", "ton", "tu", "un", "une", "vos",
    "votre", "vous", "cette", "cet", "d", "l", "the", "and", "for", "with",
    "from", "into", "your", "our", "their", "job", "poste", "profil", "plus",
    "avoir", "etre", "est", "suis", "sommes", "sont", "will", "can", "able"
}


def normalize_text(text: str) -> str:
    if not text:
        return ""
    text = unicodedata.normalize("NFKD", text)
    text = "".join(char for char in text if not unicodedata.combining(char))
    return text.lower()


def tokenize(text: str) -> list[str]:
    normalized = normalize_text(text)
    tokens = re.findall(r"[a-z0-9+#]{2,}", normalized)
    return [token for token in tokens if token not in STOPWORDS]


def load_text_from_path(path_value: str) -> str:
    path = Path(path_value)
    if not path.exists() or not path.is_file():
        return ""
    if path.suffix.lower() not in {".txt", ".md"}:
        return ""
    return path.read_text(encoding="utf-8", errors="ignore")


def cosine_similarity(counter_a: Counter, counter_b: Counter) -> float:
    if not counter_a or not counter_b:
        return 0.0

    common = set(counter_a) & set(counter_b)
    numerator = sum(counter_a[token] * counter_b[token] for token in common)
    norm_a = math.sqrt(sum(value * value for value in counter_a.values()))
    norm_b = math.sqrt(sum(value * value for value in counter_b.values()))

    if norm_a == 0 or norm_b == 0:
        return 0.0
    return numerator / (norm_a * norm_b)


def keyword_overlap(job_keywords: set[str], candidate_keywords: set[str]) -> float:
    if not job_keywords:
        return 0.0
    return len(job_keywords & candidate_keywords) / len(job_keywords)


def build_job_profile(offer: dict) -> dict:
    title = offer.get("titre", "")
    description = offer.get("description", "")

    title_tokens = tokenize(title)
    description_tokens = tokenize(description)
    all_tokens = title_tokens * 2 + description_tokens

    frequencies = Counter(all_tokens)
    important_keywords = {token for token, _ in frequencies.most_common(20)}

    return {
        "title": title,
        "description": description,
        "token_counter": frequencies,
        "keywords": important_keywords,
    }


def build_candidate_text(candidate: dict) -> str:
    cv_text = candidate.get("cv_text", "")
    if not cv_text:
        cv_path = candidate.get("cv_url", "")
        cv_text = load_text_from_path(cv_path)

    motivation = candidate.get("lettre_motivation", "")
    return "\n".join(part for part in [cv_text, motivation] if part)


def score_band(score: float) -> str:
    if score >= 75:
        return "excellent"
    if score >= 55:
        return "bon"
    if score >= 35:
        return "moyen"
    return "faible"


def build_strengths(matched_keywords: list[str], motivation_bonus: float, cosine: float) -> list[str]:
    strengths = []
    if matched_keywords:
        strengths.append("Competences alignees detectees: " + ", ".join(matched_keywords[:8]))
    if motivation_bonus:
        strengths.append("La lettre de motivation reprend des elements du poste cible.")
    if cosine >= 0.45:
        strengths.append("Le contenu global du profil est coherent avec l'offre.")
    return strengths or ["Le dossier contient quelques elements exploitables, mais l'alignement reste limite."]


def build_improvement_points(missing_keywords: list[str], candidate_text: str, motivation: str) -> list[str]:
    improvements = []
    if missing_keywords:
        improvements.append("Ajouter ou mieux mettre en valeur ces mots-cles: " + ", ".join(missing_keywords[:8]))
    if len(tokenize(motivation)) < 8:
        improvements.append("Developper une lettre de motivation plus precise, centree sur les besoins du poste.")
    if len(tokenize(candidate_text)) < 20:
        improvements.append("Enrichir le CV avec des experiences, outils, projets et resultats mesurables.")
    if "projet" not in normalize_text(candidate_text) and "experience" not in normalize_text(candidate_text):
        improvements.append("Ajouter des exemples concrets de projets ou d'experiences en lien avec le poste.")
    return improvements or ["Renforcer la personnalisation de la candidature pour cette offre."]


def build_recommendation(score: float, status: str) -> str:
    normalized_status = normalize_text(status)
    if normalized_status == "declined" and score < 35:
        return "Candidature refusee avec score faible: une repostulation est possible apres renforcement du CV et de la motivation."
    if normalized_status == "declined":
        return "Candidature refusee malgre une base exploitable: retravailler les points faibles avant de repostuler."
    if score >= 55:
        return "Profil globalement pertinent pour l'offre."
    return "Profil partiellement aligne: des ameliorations ciblees sont recommandees."


def extract_features(cv_text: str, job_title: str, job_description: str) -> list[float]:
    text = cv_text.lower()
    job = (job_title + ' ' + job_description).lower()

    cv_tokens = re.findall(r'\b[a-z0-9+#]{2,}\b', text)
    job_tokens = re.findall(r'\b[a-z0-9+#]{2,}\b', job)

    cv_freq = Counter(cv_tokens)
    job_freq = Counter(job_tokens)

    # Cosine similarity
    common = set(cv_freq) & set(job_freq)
    dot = sum(cv_freq[t] * job_freq[t] for t in common)
    norm_cv = math.sqrt(sum(v*v for v in cv_freq.values())) if cv_freq else 0
    norm_job = math.sqrt(sum(v*v for v in job_freq.values())) if job_freq else 0
    similarity = dot / (norm_cv * norm_job) if norm_cv and norm_job else 0

    # Coverage
    cv_set = set(cv_freq)
    job_set = set(job_freq)
    overlap = cv_set & job_set
    coverage = len(overlap) / len(job_set) if job_set else 0

    # Skill hits
    keywords = ['php', 'symfony', 'mysql', 'postgresql', 'sql', 'api', 'rest', 'javascript', 'typescript', 'react', 'vue', 'html', 'css', 'figma', 'ux', 'ui', 'analytics', 'marketing', 'seo', 'testing', 'qa', 'automation', 'communication', 'leadership', 'docker', 'kubernetes', 'python', 'java', 'git']
    skill_hits = sum(1 for kw in keywords if kw in cv_set)

    # Word count
    word_count = len(cv_tokens)

    # Experience years
    exp_matches = re.findall(r'(\d{1,2})\s*(ans|an|years|year)', text, re.I)
    experience_years = max([int(m[0]) for m in exp_matches] + [0])

    # Overlap count
    overlap_count = len(overlap)

    # Job tokens count
    job_tokens_count = len(job_tokens)

    return [
        similarity,
        coverage,
        min(1.0, skill_hits / 12),
        min(1.0, word_count / 900),
        min(1.0, experience_years / 15),
        min(1.0, overlap_count / 40),
        min(1.0, job_tokens_count / 300),
    ]


def detect_education_level(cv_text: str) -> str:
    """Détecte le niveau d'éducation du candidat"""
    text = cv_text.lower()

    if re.search(r'\b(doctorat|phd|ph\.d)\b', text):
        return "doctorat"
    elif re.search(r'\b(master|m\.?sc|mba)\b', text):
        return "master"
    elif re.search(r'\b(licence|bachelor|bac\+?3)\b', text):
        return "licence"
    elif re.search(r'\b(bts|dut|bac\+?2)\b', text):
        return "bac+2"
    elif re.search(r'\b(bac|baccalaureat)\b', text):
        return "baccalaureat"
    return "non-specifie"


def detect_languages(cv_text: str) -> list[str]:
    """Détecte les langues maîtrisées par le candidat"""
    text = cv_text.lower()
    languages = []

    language_patterns = {
        'francais': [r'\bfrancais\b', r'\bfran[cç]ais\b'],
        'anglais': [r'\banglais\b', r'\benglish\b'],
        'allemand': [r'\ballemand\b', r'\bgerman\b'],
        'espagnol': [r'\bespagnol\b', r'\bespanol\b', r'\bspanish\b'],
        'italien': [r'\bitalien\b', r'\bitalian\b'],
        'portugais': [r'\bportugais\b', r'\bportuguese\b'],
        'dutch': [r'\bdutch\b', r'\bneerlandais\b'],
        'russe': [r'\brusse\b', r'\brussian\b'],
        'chinois': [r'\bchinois\b', r'\bchinese\b', r'\bmandarin\b'],
        'japonais': [r'\bjaponais\b', r'\bjapanese\b'],
    }

    for lang, patterns in language_patterns.items():
        for pattern in patterns:
            if re.search(pattern, text):
                languages.append(lang)
                break

    return list(set(languages))


def detect_certifications(cv_text: str) -> list[str]:
    """Détecte les certifications pertinentes"""
    text = cv_text.lower()
    certifications = []

    cert_patterns = {
        'AWS': [r'\baws\b', r'\bamazon web services\b'],
        'Azure': [r'\bazure\b', r'\bmicrosoft azure\b'],
        'GCP': [r'\bgcp\b', r'\bgoogle cloud\b'],
        'Kubernetes': [r'\bkubernetes\b', r'\bk8s\b'],
        'Docker': [r'\bdocker\b'],
        'Scrum': [r'\bscrum\b', r'\bcsm\b'],
        'Agile': [r'\bagile\b'],
        'Security+': [r'\bsecurity\+\b'],
        'ITIL': [r'\bitil\b'],
        'PMP': [r'\bpmp\b'],
    }

    for cert, patterns in cert_patterns.items():
        for pattern in patterns:
            if re.search(pattern, text):
                certifications.append(cert)
                break

    return list(set(certifications))


def detect_soft_skills(cv_text: str) -> dict:
    """Détecte les soft skills mentionnés"""
    text = cv_text.lower()
    soft_skills = {
        'leadership': 0,
        'communication': 0,
        'teamwork': 0,
        'problem_solving': 0,
        'creativity': 0,
        'adaptability': 0,
        'project_management': 0,
        'time_management': 0,
    }

    skill_patterns = {
        'leadership': [r'\bleadership\b', r'\bleader\b', r'\bmeneur\b'],
        'communication': [r'\bcommunication\b', r'\bpresentat', r'\bcommunicat'],
        'teamwork': [r'\bteamwork\b', r'\bteam\b', r'\bequipe\b', r'\bcollaborat'],
        'problem_solving': [r'\bproblem.?solving\b', r'\bresolution\b', r'\bproblem-solving\b'],
        'creativity': [r'\bcreativity\b', r'\bcreative\b', r'\bcreatif\b'],
        'adaptability': [r'\badaptab\b', r'\badapt\b', r'\bflexib\b'],
        'project_management': [r'\bproject.?management\b', r'\bgestion.*projet\b'],
        'time_management': [r'\btime.?management\b', r'\bgest.*temps\b'],
    }

    for skill, patterns in skill_patterns.items():
        for pattern in patterns:
            if re.search(pattern, text):
                soft_skills[skill] = 1

    return soft_skills


def calculate_profile_completeness(candidate: dict) -> float:
    """Calcule le complétude du profil candidat (0-100%)"""
    cv_text = candidate.get("cv_text", "")
    motivation = candidate.get("lettre_motivation", "")

    completeness = 0.0
    max_points = 100

    # CV présent et complet
    if cv_text and len(cv_text) > 200:
        completeness += 30
    elif cv_text and len(cv_text) > 80:
        completeness += 15

    # Lettre de motivation
    if motivation and len(motivation) > 100:
        completeness += 20
    elif motivation:
        completeness += 10

    # Détection d'expérience
    if re.search(r'\d+\s*(ans|an|years|year)', cv_text.lower()):
        completeness += 15

    # Détection de formation
    if re.search(r'\b(licence|master|doctorat|bts|dut|bac)\b', cv_text.lower()):
        completeness += 15

    # Détection de compétences techniques
    if any(skill in cv_text.lower() for skill in ['python', 'java', 'api', 'sql', 'git', 'docker']):
        completeness += 20

    return min(100, completeness)


def train_model() -> RandomForestClassifier:
    # Use sample data with hardcoded labels
    sample_data = {
        "offer": {
            "titre": "Développeur Python Backend",
            "description": "Nous cherchons un développeur Python backend avec expérience en API REST, SQL, Git, tests unitaires et intégration de services IA."
        },
        "candidates": [
            {
                "candidat_id": "candidat_001",
                "cv_text": "Développeur Python avec 3 ans d'expérience. Création d'API REST avec Flask et FastAPI. Bonne maîtrise SQL, Git, tests unitaires et Docker.",
                "lettre_motivation": "Je souhaite rejoindre votre équipe backend Python et travailler sur des API robustes.",
                "status": "accepted"
            },
            {
                "candidat_id": "candidat_002",
                "cv_text": "Développeur front-end React et JavaScript. Intégration HTML CSS, UX, composants web, notions d'API.",
                "lettre_motivation": "Je suis motivé pour évoluer vers le développement full-stack.",
                "status": "declined"
            },
            {
                "candidat_id": "candidat_003",
                "cv_text": "Ingénieur logiciel Java et Spring Boot. Expérience SQL, Git, microservices, tests JUnit et consommation d'API.",
                "lettre_motivation": "Mon profil backend peut répondre à votre besoin même si Python n'est pas ma stack principale.",
                "status": "declined"
            }
        ]
    }

    samples = []
    labels = []
    for candidate in sample_data['candidates']:
        cv_text = candidate['cv_text'] + ' ' + candidate['lettre_motivation']
        features = extract_features(cv_text, sample_data['offer']['titre'], sample_data['offer']['description'])
        samples.append(features)
        labels.append(1 if candidate['status'] == 'accepted' else 0)

    # Train
    clf = RandomForestClassifier(n_estimators=10, random_state=42)
    clf.fit(samples, labels)

    # Save model
    with open('ml_model.pkl', 'wb') as f:
        pickle.dump(clf, f)

    return clf


def load_model() -> RandomForestClassifier:
    if os.path.exists('ml_model.pkl'):
        with open('ml_model.pkl', 'rb') as f:
            return pickle.load(f)
    else:
        return train_model()


def score_candidate(candidate: dict, job_profile: dict) -> dict:
    candidate_text = build_candidate_text(candidate)
    candidate_counter = Counter(tokenize(candidate_text))
    candidate_keywords = set(candidate_counter)

    cosine = cosine_similarity(job_profile["token_counter"], candidate_counter)
    overlap = keyword_overlap(job_profile["keywords"], candidate_keywords)

    motivation_tokens = tokenize(candidate.get("lettre_motivation", ""))
    title_tokens = set(tokenize(job_profile["title"]))
    motivation_bonus = 1.0 if title_tokens and title_tokens & set(motivation_tokens) else 0.0

    heuristic_score = (
        cosine * 65
        + overlap * 30
        + motivation_bonus * 5
    )

    # ML prediction
    try:
        model = load_model()
        features = extract_features(candidate_text, job_profile["title"], job_profile["description"])
        proba = model.predict_proba([features])[0]
        accepted_proba = proba[1]  # assuming class 1 is accepted

        raw_model_score = accepted_proba * 100
        text_length = len(tokenize(candidate_text))
        if text_length < 50:
            raw_model_score = min(raw_model_score, 58)
        elif text_length < 100:
            raw_model_score = min(raw_model_score, 72)

        confidence = abs(accepted_proba - 0.5) * 2
        text_quality = min(1.0, text_length / 300)
        model_weight = 0.35 + 0.45 * confidence + 0.20 * text_quality
        model_weight = max(0.25, min(0.85, model_weight))

        final_score = raw_model_score * model_weight + heuristic_score * (1 - model_weight)

        ml_details = {
            "accepted_probability": round(accepted_proba * 100, 2),
            "raw_model_score": round(raw_model_score, 2),
            "heuristic_score": round(heuristic_score, 2),
            "model_weight": round(model_weight, 3),
            "confidence": round(confidence, 3),
            "text_quality": round(text_quality, 3),
        }
    except Exception as e:
        # Fallback to heuristic
        final_score = heuristic_score
        ml_details = {"error": str(e)}

    matched_keywords = sorted(job_profile["keywords"] & candidate_keywords)
    missing_keywords = sorted(job_profile["keywords"] - candidate_keywords)
    status = candidate.get("status", "")
    band = score_band(final_score)

    # Nouvelles analyses
    education_level = detect_education_level(candidate_text)
    languages = detect_languages(candidate_text)
    certifications = detect_certifications(candidate_text)
    soft_skills = detect_soft_skills(candidate_text)
    profile_completeness = calculate_profile_completeness(candidate)

    details = {
        "cosine_similarity": round(cosine, 4),
        "keyword_overlap": round(overlap, 4),
        "motivation_bonus": motivation_bonus,
        "matched_keywords": matched_keywords[:15],
        "missing_keywords": missing_keywords[:15],
        "strengths": build_strengths(matched_keywords, motivation_bonus, cosine),
        "improvement_points": build_improvement_points(
            missing_keywords,
            candidate_text,
            candidate.get("lettre_motivation", "")
        ),
        "recommendation": build_recommendation(final_score, status),
        "education_level": education_level,
        "languages": languages,
        "certifications": certifications,
        "soft_skills": {k: v for k, v in soft_skills.items() if v == 1},
        "profile_completeness_percentage": round(profile_completeness, 1),
    }
    if "error" not in ml_details:
        details.update(ml_details)

    return {
        "candidat_id": candidate.get("candidat_id"),
        "status": status,
        "score": round(final_score, 2),
        "score_band": band,
        "details": details,
    }


def rank_candidates(payload: dict) -> dict:
    offer = payload.get("offer", {})
    candidates = payload.get("candidates", [])

    job_profile = build_job_profile(offer)
    results = [score_candidate(candidate, job_profile) for candidate in candidates]
    results.sort(key=lambda item: item["score"], reverse=True)

    return {
        "offer": {
            "titre": offer.get("titre"),
            "description": offer.get("description"),
        },
        "ranking": results,
        "best_match": results[0] if results else None,
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Calcule un score de matching entre une offre et plusieurs candidats."
    )
    parser.add_argument(
        "input_json",
        help="Chemin vers un fichier JSON contenant 'offer' et 'candidates'.",
    )
    parser.add_argument(
        "--pretty",
        action="store_true",
        help="Affiche le JSON formaté.",
    )
    parser.add_argument(
        "--text",
        action="store_true",
        help="Affiche un résumé lisible du classement.",
    )
    return parser.parse_args()


def to_text_report(result: dict) -> str:
    offer = result.get("offer", {})
    ranking = result.get("ranking", [])

    lines = [
        f"Offre: {offer.get('titre', '')}",
        f"Description: {offer.get('description', '')}",
        "",
        "Classement des candidats:",
    ]

    if not ranking:
        lines.append("Aucun candidat à classer.")
        return "\n".join(lines)

    for index, item in enumerate(ranking, start=1):
        details = item.get("details", {})
        keywords = ", ".join(details.get("matched_keywords", [])) or "aucun"
        missing_keywords = ", ".join(details.get("missing_keywords", [])) or "aucun"
        strengths = details.get("strengths", [])
        improvements = details.get("improvement_points", [])
        lines.extend(
            [
                f"{index}. {item.get('candidat_id')} - score {item.get('score')}/100",
                f"   statut: {item.get('status') or 'non defini'}",
                f"   niveau: {item.get('score_band')}",
                f"   similarite cosinus: {details.get('cosine_similarity')}",
                f"   chevauchement mots-cles: {details.get('keyword_overlap')}",
                f"   bonus motivation: {details.get('motivation_bonus')}",
                f"   mots-cles communs: {keywords}",
                f"   mots-cles manquants: {missing_keywords}",
            ]
        )
        if 'accepted_probability' in details:
            lines.extend([
                f"   probabilite acceptance ML: {details.get('accepted_probability')}%",
                f"   score brut ML: {details.get('raw_model_score')}",
                f"   score heuristique: {details.get('heuristic_score')}",
                f"   poids du modele: {details.get('model_weight')}",
                f"   confiance: {details.get('confidence')}",
                f"   qualite texte: {details.get('text_quality')}",
            ])
        lines.append("   points forts:")
        for strength in strengths:
            lines.append(f"   - {strength}")
        lines.append("   points a ameliorer:")
        for improvement in improvements:
            lines.append(f"   - {improvement}")
        lines.append(f"   recommandation: {details.get('recommendation')}")

    best_match = result.get("best_match")
    if best_match:
        lines.extend(
            [
                "",
                f"Meilleur candidat: {best_match.get('candidat_id')} avec un score de {best_match.get('score')}/100",
            ]
        )

    return "\n".join(lines)


def main() -> None:
    args = parse_args()
    payload = json.loads(Path(args.input_json).read_text(encoding="utf-8-sig"))
    result = rank_candidates(payload)

    if args.text:
        print(to_text_report(result))
    elif args.pretty:
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
