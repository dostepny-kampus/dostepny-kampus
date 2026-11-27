/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.em
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.GroupContainer
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.account.nameRes
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.card.InfoPanel
import io.github.dostepnykampus.core.designsystem.readableWidth
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole

private const val LINK_ID = "externalLink"
private const val LINK_EM = 1.2f

private val ClauseSections =
    listOf(
        R.string.konto_klauzula_cel_naglowek to R.string.konto_klauzula_cel_tresc,
        R.string.konto_klauzula_podstawa_naglowek to R.string.konto_klauzula_podstawa_tresc,
        R.string.konto_klauzula_czas_naglowek to R.string.konto_klauzula_czas_tresc,
        R.string.konto_klauzula_prawa_naglowek to R.string.konto_klauzula_prawa_tresc,
    )

private val ClauseDataItems =
    listOf(R.string.konto_klauzula_dane_konto, R.string.konto_klauzula_dane_role, R.string.konto_klauzula_dane_zdjecia)

/** Strona repozytorium projektu na GitHubie, cel odnośnika kontaktowego w klauzuli informacyjnej. */
internal const val REPOSITORY_URL = "https://github.com/dostepny-kampus/dostepny-kampus"

private enum class AccountView { MENU, PRIVACY_NOTICE, WITHDRAW_CONSENT }

/**
 * L2 „Konto” zalogowanej osoby: dane konta, czynności posiadanych ról, klauzula informacyjna, wycofanie zgody
 * i wylogowanie.
 *
 * Wymagania: US14.1, US14.3, US14.4, BR-1, NFR-29, NFR-30
 *
 * @param account zalogowane konto
 * @param actions czynności prowadzące poza ekran
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun AccountScreen(account: Account.SignedIn, actions: AccountActions, modifier: Modifier = Modifier) {
    val title = if (account.staffRoles.isEmpty()) R.string.logowanie_naglowek else R.string.konto_i_praca
    var view by rememberSaveable { mutableStateOf(AccountView.MENU) }
    val returnToMenu = { view = AccountView.MENU }
    BackHandler(enabled = view != AccountView.MENU, onBack = returnToMenu)
    val onBack = if (view == AccountView.MENU) actions.onBack else returnToMenu
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.readableWidth().windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(title), onBack = onBack)
            key(view) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m)) {
                    when (view) {
                        AccountView.MENU -> { AccountMenu(account = account, actions = actions) { view = it } }

                        AccountView.PRIVACY_NOTICE -> { AccountPrivacyNotice() }

                        AccountView.WITHDRAW_CONSENT -> {
                            AccountWithdrawConsent(onWithdraw = actions.onSignOut, onKeep = returnToMenu)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountMenu(account: Account.SignedIn, actions: AccountActions, onOpen: (AccountView) -> Unit) {
    val work = WorkAction.entries.filter { it.role in account.staffRoles }
    PersonCard(account)
    if (work.isNotEmpty()) {
        GroupContainer(title = stringResource(R.string.konto_twoja_praca)) {
            work.forEachIndexed { index, action ->
                if (index > 0) HorizontalDivider()
                ActionRow(AccountIcons.iconOf(action), action.label) { actions.onWork(action) }
            }
        }
    }
    GroupContainer(title = stringResource(R.string.logowanie_naglowek)) {
        ActionRow(AccountIcons.PrivacyNotice, R.string.konto_klauzula) { onOpen(AccountView.PRIVACY_NOTICE) }
        HorizontalDivider()
        ActionRow(AccountIcons.WithdrawConsent, R.string.konto_wycofaj_zgode) {
            onOpen(AccountView.WITHDRAW_CONSENT)
        }
        HorizontalDivider()
        ActionRow(
            icon = AccountIcons.SignOut,
            label = R.string.konto_wyloguj,
            hasForward = false,
            onClick = actions.onSignOut,
        )
    }
}

@Composable
private fun AccountPrivacyNotice() {
    val body = MaterialTheme.typography.bodyLarge
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
        Text(
            text = stringResource(R.string.konto_klauzula),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        InfoPanel {
            Column(verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
                ClauseSection(R.string.konto_klauzula_administrator_naglowek) {
                    Text(stringResource(R.string.konto_klauzula_administrator_tresc), style = body)
                }
                ClauseSection(R.string.konto_klauzula_kontakt_naglowek) {
                    Text(stringResource(R.string.konto_klauzula_kontakt_wstep), style = body)
                    RepositoryLink()
                }
                ClauseSection(R.string.konto_klauzula_dane_naglowek) {
                    ClauseDataItems.forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s)) {
                            Text(text = "–", style = body)
                            Text(text = stringResource(item), style = body)
                        }
                    }
                }
                ClauseSections.forEach { (title, text) ->
                    ClauseSection(title) { Text(stringResource(text), style = body) }
                }
            }
        }
        Text(text = stringResource(R.string.konto_klauzula_rejestracja), style = body)
    }
}

@Composable
private fun AccountWithdrawConsent(onWithdraw: () -> Unit, onKeep: () -> Unit) {
    InfoPanel {
        Text(
            text = stringResource(R.string.konto_wycofanie_tytul),
            modifier = Modifier.semantics { heading() },
            fontWeight = Typografia.wyroznienieWaga,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(text = stringResource(R.string.konto_wycofanie_opis), style = MaterialTheme.typography.bodyLarge)
        Column(Modifier.padding(top = Odstepy.s), verticalArrangement = Arrangement.spacedBy(CeleDotyku.odstep)) {
            val buttonModifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk)
            val labelStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = Typografia.wyroznienieWaga)
            val choices = listOf(R.string.konto_wycofaj_zgode to onWithdraw, R.string.konto_zostaw_zgode to onKeep)
            for ((label, onClick) in choices) {
                FilledTonalButton(onClick, buttonModifier, shape = MaterialTheme.shapes.extraLarge) {
                    Text(text = stringResource(label), style = labelStyle, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun ClauseSection(
    @StringRes title: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.xs)) {
        Text(stringResource(title), Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun RepositoryLink() {
    val color = MaterialTheme.colorScheme.primary
    val span = SpanStyle(color, fontWeight = Typografia.wyroznienieWaga, textDecoration = TextDecoration.Underline)
    val label = stringResource(R.string.konto_klauzula_kontakt_odnosnik)
    val text =
        buildAnnotatedString {
            withLink(LinkAnnotation.Url(url = REPOSITORY_URL, styles = TextLinkStyles(span))) {
                append("$label ")
                appendInlineContent(id = LINK_ID, alternateText = " ")
            }
        }
    val box = Placeholder(LINK_EM.em, LINK_EM.em, PlaceholderVerticalAlign.TextCenter)
    val icon =
        InlineTextContent(box) {
            Icon(
                imageVector = AccountIcons.ExternalLink,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = color,
            )
        }
    Text(
        text = text,
        modifier = Modifier.padding(top = Odstepy.xs),
        inlineContent = mapOf(LINK_ID to icon),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun PersonCard(account: Account.SignedIn) {
    val names = listOf(stringResource(R.string.rola_odwiedzajacy)) + roleNames(account)
    InfoPanel {
        if (account.displayName.isNotBlank()) {
            Text(text = account.displayName, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = stringResource(R.string.konto_twoje_role, names.joinToString(separator = ", ")),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
@ReadOnlyComposable
private fun roleNames(account: Account.SignedIn): List<String> =
    StaffRole.entries
        .filter { it in account.staffRoles }
        .map { role -> stringResource(role.nameRes()) }

@Composable
private fun ActionRow(
    icon: ImageVector,
    @StringRes label: Int,
    hasForward: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = CeleDotyku.minimum)
                .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(Odstepy.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Text(
            text = stringResource(label),
            modifier = Modifier.weight(1f).padding(vertical = Odstepy.s),
            fontWeight = Typografia.wyroznienieWaga,
            style = MaterialTheme.typography.titleMedium,
        )
        if (hasForward) Icon(imageVector = AccountIcons.Forward, contentDescription = null)
    }
}
