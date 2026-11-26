/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.GroupContainer
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.account.nameRes
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.card.InfoPanel
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole

/**
 * L2 „Konto” zalogowanej osoby: karta z rolami, czynności posiadanych ról i sekcja konta.
 *
 * TODO(#3773): klauzula informacyjna i wycofanie zgody.
 * TODO(#3774): nazwa wyświetlana na karcie osoby.
 *
 * Wymagania: US14.1, US14.3, US14.4, BR-1
 *
 * @param account zalogowane konto
 * @param actions czynności prowadzące poza ekran
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun AccountScreen(account: Account.SignedIn, actions: AccountActions, modifier: Modifier = Modifier) {
    val work = WorkAction.entries.filter { it.role in account.staffRoles }
    val title = if (account.staffRoles.isEmpty()) R.string.logowanie_naglowek else R.string.konto_i_praca
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(title), onBack = actions.onBack)
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m)) {
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
                    ActionRow(
                        icon = AccountIcons.SignOut,
                        label = R.string.konto_wyloguj,
                        hasForward = false,
                        onClick = actions.onSignOut,
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonCard(account: Account.SignedIn) {
    val names = listOf(stringResource(R.string.rola_odwiedzajacy)) + roleNames(account)
    InfoPanel {
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
