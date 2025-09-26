# Play Store Publication Assets

This folder contains all the documentation and assets needed to publish HowManyHours on Google Play Store.

## Folder Structure

### 📋 `/legal/`
- **privacy-policy.md** - Complete privacy policy text (host this online)
- **data-safety-disclosure.md** - Google Play Data Safety section responses  
- **content-rating-guide.md** - Content rating questionnaire guide

### 📱 `/screenshots/`
- **screenshot-requirements.md** - Technical requirements and guidelines
- *[Add your actual screenshots here when ready]*

### 📝 `/descriptions/`
- **store-listing.md** - App title, descriptions, and store copy

### 🔧 `/technical-docs/`
- **data-lifecycle-analysis.md** - Complete technical analysis of data storage
- **release-preparation.md** - Step-by-step release checklist

## Key Actions Required

### Before Publishing:
1. **Host Privacy Policy** - Upload `privacy-policy.md` to a public URL
2. **Take Screenshots** - Follow guidelines in `/screenshots/screenshot-requirements.md`
3. **Create App Icon** - 512x512px PNG following Material Design
4. **Generate Signed APK/AAB** - Follow `/technical-docs/release-preparation.md`

### Technical Summary:
- **Storage**: Android Room (SQLite) - Local only, no cloud sync
- **Data**: Project names, time entries, timestamps
- **Privacy**: Complete privacy - no external data transmission
- **Security**: Android app sandboxing protection
- **Compliance**: GDPR-friendly, minimal data collection

### App Classification:
- **Category**: Business/Productivity
- **Rating**: Everyone (safe for all audiences)  
- **Permissions**: Minimal (notifications, foreground service)
- **Data Safety**: Only local user-generated content (time tracking)

## Quick Start Guide

1. Review all documents in this folder
2. Create Google Play Developer account ($25)
3. Host privacy policy online
4. Take required screenshots using your app
5. Follow release preparation checklist
6. Submit to Play Store

**Estimated Total Time**: 1-2 weeks for preparation + 1-3 days for Google review

---

*All documents are templates - customize with your specific information before use.*