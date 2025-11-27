const tagState = {};
    let isEditing = false;
    const heroStats = {};
    const githubModal = {overlay: null, input: null};

    // Toast notification utility (local copy to ensure availability)
    function showToast(cls, msg, delay = 4000) {
        const container = document.querySelector('.toast-container') || (function () {
            const c = document.createElement('div');
            c.className = 'toast-container position-fixed top-0 end-0 p-3';
            c.style.zIndex = '1300';
            document.body.appendChild(c);
            return c;
        })();
        const t = document.createElement('div');
        t.className = 'toast align-items-center ' + cls + ' border-0';
        t.setAttribute('role', 'alert');
        t.innerHTML = '<div class="d-flex"><div class="toast-body">' + msg + '</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div>';
        container.appendChild(t);
        const toast = new bootstrap.Toast(t, {delay: delay});
        toast.show();
        // Remove toast element after it's hidden
        t.addEventListener('hidden.bs.toast', () => t.remove());
    }

    document.addEventListener('DOMContentLoaded', () => {
        ['skills', 'softSkills'].forEach(initTagInput);
        setupHeroStats();
        githubModal.overlay = document.getElementById('githubImportModal');
        githubModal.input = document.getElementById('githubUsernameInput');
        if (githubModal.overlay) {
            githubModal.overlay.addEventListener('click', (event) => {
                if (event.target === githubModal.overlay) {
                    closeGithubImportModal();
                }
            });
        }

        // CRITICAL FIX: Remove empty file input AND multipart enctype to avoid FileCountLimitExceededException
        const profileForm = document.getElementById('profileForm');
        if (profileForm) {
            console.log('[MULTIPART FIX] Profile form found, attaching submit handler');

            profileForm.addEventListener('submit', function (event) {
                console.log('[MULTIPART FIX] Form submit triggered');
                const avatarInput = profileForm.querySelector('input[name="avatar"]');

                if (avatarInput) {
                    const hasFiles = avatarInput.files && avatarInput.files.length > 0;
                    console.log('[MULTIPART FIX] Avatar input found. Has files:', hasFiles);

                    if (!hasFiles) {
                        console.log('[MULTIPART FIX] NO FILE - Removing empty avatar input');
                        avatarInput.remove();

                        // CRITICAL: Remove multipart enctype to send as normal POST
                        console.log('[MULTIPART FIX] Removing enctype="multipart/form-data"');
                        profileForm.removeAttribute('enctype');
                        console.log('[MULTIPART FIX] Form will be submitted as application/x-www-form-urlencoded');
                    } else {
                        console.log('[MULTIPART FIX] HAS FILE - Keeping avatar input and multipart enctype');
                    }
                } else {
                    console.log('[MULTIPART FIX] Avatar input not found');
                }
            });
        } else {
            console.error('[MULTIPART FIX] Profile form NOT found!');
        }

        setEditMode(false);
    });

    function setupHeroStats() {
        ['repos', 'followers', 'following'].forEach((metric) => {
            heroStats[metric] = document.querySelector(`[data-hero-stat="${metric}"]`);
        });
    }

    function setEditMode(enabled) {
        isEditing = enabled;
        document.body.dataset.editing = enabled ? 'true' : 'false';
        document.querySelectorAll('[data-editable-field]').forEach((field) => {
            if (field.type === 'hidden') {
                return;
            }
            if (field.tagName === 'SELECT' || field.type === 'file') {
                field.disabled = !enabled;
            } else {
                field.readOnly = !enabled;
            }
            field.classList.toggle('read-only', !enabled);
        });
        document.querySelectorAll('[data-edit-visible]').forEach((el) => {
            el.style.display = enabled ? '' : 'none';
        });
        document.querySelectorAll('[data-view-visible]').forEach((el) => {
            el.style.display = enabled ? 'none' : '';
        });
        document.querySelectorAll('.tag-input-field').forEach((input) => {
            input.disabled = !enabled;
            if (!input.dataset.placeholderActive) {
                input.dataset.placeholderActive = input.getAttribute('placeholder') || '';
            }
            input.placeholder = enabled
                ? input.dataset.placeholderActive
                : 'Ative a edição para adicionar novas tags';
        });
        if (enabled) {
            const firstField = document.querySelector('[data-editable-field]:not([type="hidden"])');
            if (firstField) {
                firstField.focus({preventScroll: false});
            }
        }
    }

    function enableEditMode() {
        setEditMode(true);
    }

    function cancelEditMode() {
        setEditMode(false);
        window.location.reload();
    }

    function openGithubImportModal() {
        if (!githubModal.overlay) {
            return;
        }
        const loginField = document.querySelector('[name="githubLogin"]');
        const urlField = document.querySelector('[name="githubUrl"]');
        const suggestion = (loginField && loginField.value)
            || extractGithubUsername((urlField && urlField.value) || '');
        if (githubModal.input) {
            githubModal.input.value = suggestion || '';
        }
        githubModal.overlay.classList.add('open');
        setTimeout(() => {
            if (githubModal.input) {
                githubModal.input.focus();
            }
        }, 120);
    }

    function closeGithubImportModal() {
        if (githubModal.overlay) {
            githubModal.overlay.classList.remove('open');
        }
    }

    function confirmGithubImport() {
        if (!githubModal.input) {
            return;
        }
        const username = githubModal.input.value.trim();
        if (!username) {
            githubModal.input.focus();
            return;
        }
        closeGithubImportModal();
        if (!isEditing) {
            setEditMode(true);
        }
        importFromGithub(username);
    }

    function initTagInput(field) {
        const hidden = document.querySelector(`[data-tag-hidden="${field}"]`);
        const list = document.querySelector(`[data-tag-list="${field}"]`);
        const input = document.querySelector(`[data-tag-input="${field}"]`);
        if (!hidden || !list || !input) {
            return;
        }
        tagState[field] = hidden.value
            ? hidden.value.split(',').map((tag) => tag.trim()).filter((tag) => tag.length > 0)
            : [];
        input.dataset.placeholderActive = input.getAttribute('placeholder') || '';
        renderTags(field, list, hidden);

        input.addEventListener('keydown', (event) => {
            if (!isEditing) {
                return;
            }
            if ((event.key === 'Enter' || event.key === ',') && input.value.trim()) {
                event.preventDefault();
                addTag(field, input.value.trim(), list, hidden);
                input.value = '';
            }
        });
    }

    function renderTags(field, list, hidden) {
        list.innerHTML = '';
        tagState[field].forEach((tag, index) => {
            const chip = document.createElement('span');
            chip.className = 'tag-chip';
            const label = document.createElement('span');
            label.textContent = tag;
            chip.appendChild(label);
            const remove = document.createElement('button');
            remove.type = 'button';
            remove.innerHTML = '&times;';
            remove.setAttribute('aria-label', `Remover ${tag}`);
            remove.addEventListener('click', () => {
                if (!isEditing) return;
                removeTag(field, index, list, hidden);
            });
            chip.appendChild(remove);
            list.appendChild(chip);
        });
        hidden.value = tagState[field].join(', ');
    }

    function addTag(field, value, list, hidden) {
        if (!isEditing) {
            return;
        }
        const normalized = value.trim();
        if (!normalized || tagState[field].some((tag) => tag.toLowerCase() === normalized.toLowerCase())) {
            return;
        }
        tagState[field].push(normalized);
        renderTags(field, list, hidden);
    }

    function removeTag(field, index, list, hidden) {
        tagState[field].splice(index, 1);
        renderTags(field, list, hidden);
    }

    function previewAvatar(evt) {
        const file = evt.target.files && evt.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = () => {
            const img = document.getElementById('avatarPreview');
            if (img) img.src = reader.result;
        };
        reader.readAsDataURL(file);
    }

    function extractGithubUsername(url) {
        if (!url) return '';
        try {
            const u = new URL(url);
            if (!u.hostname.includes('github.com')) return '';
            const parts = u.pathname.split('/').filter(Boolean);
            return parts[0] || '';
        } catch (e) {
            return url.trim();
        }
    }

    function updateHeroStat(key, value) {
        if (heroStats[key]) {
            heroStats[key].textContent = Number(value || 0);
        }
    }

    async function importFromGithub(forcedUsername) {
        const githubInput = document.querySelector('[name="githubUrl"]');
        const urlOrUsername = githubInput ? githubInput.value : '';
        const derivedUsername = extractGithubUsername(urlOrUsername);
        const username = forcedUsername || derivedUsername;
        if (!username) {
            showToast(
                'text-bg-warning',
                '<i class="bi bi-info-circle me-2"></i>Informe o usuário do GitHub para continuar.',
                4000
            );
            return;
        }
        try {
            const resp = await fetch(`/api/github/user?username=${encodeURIComponent(username)}`);
            if (!resp.ok) throw new Error('Falha ao consultar GitHub');
            const data = await resp.json();

            const bio = document.querySelector('[name="bio"]');
            const website = document.querySelector('[name="websiteUrl"]');
            const location = document.querySelector('[name="location"]');
            const gh = document.querySelector('[name="githubUrl"]');
            const avatarUrlHidden = document.querySelector('[name="avatarUrl"]');

            const githubLogin = document.querySelector('[name="githubLogin"]');
            const githubName = document.querySelector('[name="githubName"]');
            const githubCompany = document.querySelector('[name="githubCompany"]');
            const twitterUsername = document.querySelector('[name="twitterUsername"]');
            const hireable = document.querySelector('[name="hireable"]');
            const publicRepos = document.querySelector('[name="publicRepos"]');
            const publicGists = document.querySelector('[name="publicGists"]');
            const followers = document.querySelector('[name="followers"]');
            const following = document.querySelector('[name="following"]');
            const githubCreatedAt = document.querySelector('[name="githubCreatedAt"]');
            const githubUpdatedAt = document.querySelector('[name="githubUpdatedAt"]');

            if (githubName && data.name) githubName.value = data.name;
            if (githubLogin && data.login) githubLogin.value = data.login;

            if (bio && data.bio) bio.value = data.bio;
            if (website && data.blog) website.value = data.blog;
            if (location && data.location) location.value = data.location;
            if (gh && data.html_url) gh.value = data.html_url;

            if (githubCompany && data.company) githubCompany.value = data.company;
            if (twitterUsername && data.twitter_username) twitterUsername.value = data.twitter_username;
            if (hireable) hireable.value = data.hireable ? 'true' : 'false';
            if (publicRepos) {
                publicRepos.value = data.public_repos || 0;
                updateHeroStat('repos', data.public_repos);
            }
            if (publicGists) publicGists.value = data.public_gists || 0;
            if (followers) {
                followers.value = data.followers || 0;
                updateHeroStat('followers', data.followers);
            }
            if (following) {
                following.value = data.following || 0;
                updateHeroStat('following', data.following);
            }
            if (githubCreatedAt && data.created_at) githubCreatedAt.value = data.created_at;
            if (githubUpdatedAt && data.updated_at) githubUpdatedAt.value = data.updated_at;

            if (data.avatar_url) {
                const img = document.getElementById('avatarPreview');
                if (img) img.src = data.avatar_url;
                if (avatarUrlHidden) avatarUrlHidden.value = data.avatar_url;
            }

            showToast(
                'text-bg-success',
                '<i class="bi bi-github me-2"></i><strong>GitHub sincronizado!</strong> Dados de <strong>' + (data.name || data.login) + '</strong> importados. Clique em "Salvar alterações" para confirmar.',
                5000
            );
        } catch (e) {
            showToast(
                'text-bg-danger',
                '<i class="bi bi-exclamation-triangle me-2"></i>Não foi possível importar dados do GitHub. Verifique o nome de usuário.',
                5000
            );
            console.error(e);
        }
    }

    // Expose functions to global scope for onclick handlers
    window.enableEditMode = enableEditMode;
    window.cancelEditMode = cancelEditMode;
    window.openGithubImportModal = openGithubImportModal;
    window.closeGithubImportModal = closeGithubImportModal;
    window.confirmGithubImport = confirmGithubImport;
    window.previewAvatar = previewAvatar;