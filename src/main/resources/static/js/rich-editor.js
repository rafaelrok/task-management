// Rich Editor JS Logic - Complete rewrite based on user's code
(function () {
    const HL_READY = typeof hljs !== 'undefined';

    window.RichEditor = function (rootId, hiddenFieldName) {
        const root = document.getElementById(rootId);
        if (!root) {
            console.error('Rich Editor: root element not found:', rootId);
            return;
        }

        const editor = root.querySelector('.editor-area');
        const modal = root.querySelector('.code-modal');
        const langSelect = modal ? modal.querySelector('.code-modal-select') : null;
        const codeTA = modal ? modal.querySelector('.code-modal-textarea') : null;
        const closeBtn = root.querySelector('.rich-editor-close');
        const saveBtn = root.querySelector('.footer-btn.btn-save');
        const cancelBtn = root.querySelector('.footer-btn.btn-cancel');
        const hiddenField = document.getElementById(`${rootId}_hidden`) || document.querySelector(`input[name="${hiddenFieldName}"]`);

        if (!editor) {
            console.error('Rich Editor: editor-area not found');
            return;
        }

        // Function to format text
        function formatText(command, value = null) {
            console.log('formatText called:', command, value);
            editor.focus();
            document.execCommand(command, false, value);
            editor.focus();
        }

        // Function to insert link
        function insertLink() {
            const url = prompt('Digite a URL:');
            if (url) {
                formatText('createLink', url);
            }
        }

        // Function to open code modal
        function openCodeModal() {
            console.log('openCodeModal called, modal:', modal);
            if (!modal) return;
            modal.classList.add('active');
            if (codeTA) codeTA.value = '';
        }

        // Function to close code modal
        function closeCodeModal() {
            console.log('closeCodeModal called');
            if (modal) modal.classList.remove('active');
        }

        // Function to copy code
        function copyCode(button) {
            const codeBlock = button.closest('.code-block-container').querySelector('code');
            const lines = codeBlock.querySelectorAll('.line');
            const code = Array.from(lines).map(line => line.textContent).join('\n');

            navigator.clipboard.writeText(code).then(() => {
                const originalText = button.textContent;
                button.textContent = 'Copiado!';
                setTimeout(() => {
                    button.textContent = originalText;
                }, 2000);
            });
        }

        // Function to create code block
        function createCodeBlock(language, code) {
            const codeContainer = document.createElement('div');
            codeContainer.className = 'code-block-container';
            codeContainer.contentEditable = 'false';

            // Header
            const header = document.createElement('div');
            header.className = 'code-block-header';

            const langSpan = document.createElement('span');
            langSpan.className = 'code-block-language';
            langSpan.textContent = language.toUpperCase();

            const copyBtn = document.createElement('button');
            copyBtn.className = 'code-block-copy';
            copyBtn.textContent = 'Copiar';
            copyBtn.type = 'button';
            copyBtn.onclick = function () {
                copyCode(this);
            };

            header.appendChild(langSpan);
            header.appendChild(copyBtn);

            // Content
            const content = document.createElement('div');
            content.className = 'code-block-content';

            const pre = document.createElement('pre');
            const codeElement = document.createElement('code');
            codeElement.className = `language-${language}`;

            // Define o texto do código diretamente (sem HTML)
            codeElement.textContent = code;

            pre.appendChild(codeElement);
            content.appendChild(pre);

            // Monta o bloco completo
            codeContainer.appendChild(header);
            codeContainer.appendChild(content);

            // Aplica syntax highlighting
            if (HL_READY) {
                hljs.highlightElement(codeElement);
            }

            // Adiciona a numeração de linhas após o highlighting
            const highlightedHTML = codeElement.innerHTML;
            const lines = highlightedHTML.split('\n');
            codeElement.innerHTML = '';

            lines.forEach((line) => {
                const lineSpan = document.createElement('span');
                lineSpan.className = 'line';
                lineSpan.innerHTML = line || ' ';
                codeElement.appendChild(lineSpan);
            });

            return codeContainer;
        }

        // Function to insert code block
        function insertCodeBlock() {
            if (!langSelect || !codeTA) return;

            const language = langSelect.value;
            const code = codeTA.value;

            if (!code.trim()) {
                alert('Por favor, insira algum código.');
                return;
            }

            // Garante que o editor está focado
            editor.focus();

            // Pega a seleção atual
            let selection = window.getSelection();
            let range;

            if (selection.rangeCount > 0) {
                range = selection.getRangeAt(0);

                // Se houver texto selecionado, remove
                range.deleteContents();

                // Verifica se estamos dentro de um elemento de texto
                let container = range.commonAncestorContainer;

                // Se estamos em um nó de texto, precisamos quebrá-lo
                if (container.nodeType === Node.TEXT_NODE) {
                    container = container.parentNode;
                }

                // Se estamos no meio de um parágrafo, divide ele
                if (container.nodeName !== 'DIV' || !container.classList.contains('editor-area')) {
                    // Encontra o elemento de bloco mais próximo (p, div, etc)
                    let blockElement = container;
                    while (blockElement && blockElement !== editor &&
                    !['P', 'DIV', 'H1', 'H2', 'H3', 'H4', 'BLOCKQUOTE'].includes(blockElement.nodeName)) {
                        blockElement = blockElement.parentNode;
                    }

                    if (blockElement && blockElement !== editor) {
                        // Extrai o conteúdo após o cursor
                        const afterRange = range.cloneRange();
                        afterRange.setEndAfter(blockElement.lastChild || blockElement);
                        const afterContent = afterRange.extractContents();

                        // Cria o bloco de código
                        const codeContainer = createCodeBlock(language, code);

                        // Insere o bloco após o elemento atual
                        if (blockElement.nextSibling) {
                            editor.insertBefore(codeContainer, blockElement.nextSibling);
                        } else {
                            editor.appendChild(codeContainer);
                        }

                        // Se havia conteúdo depois do cursor, cria um novo parágrafo
                        if (afterContent.textContent.trim() || afterContent.childNodes.length > 0) {
                            const newPara = document.createElement('p');
                            newPara.appendChild(afterContent);
                            if (codeContainer.nextSibling) {
                                editor.insertBefore(newPara, codeContainer.nextSibling);
                            } else {
                                editor.appendChild(newPara);
                            }

                            // Posiciona o cursor no início do novo parágrafo
                            range.setStart(newPara, 0);
                            range.collapse(true);
                        } else {
                            // Cria um parágrafo vazio após o código
                            const afterPara = document.createElement('p');
                            afterPara.innerHTML = '<br>';
                            if (codeContainer.nextSibling) {
                                editor.insertBefore(afterPara, codeContainer.nextSibling);
                            } else {
                                editor.appendChild(afterPara);
                            }

                            // Posiciona o cursor no parágrafo vazio
                            range.setStart(afterPara, 0);
                            range.collapse(true);
                        }
                    } else {
                        // Estamos diretamente no editor, insere normalmente
                        const codeContainer = createCodeBlock(language, code);
                        range.insertNode(codeContainer);

                        const afterPara = document.createElement('p');
                        afterPara.innerHTML = '<br>';
                        if (codeContainer.nextSibling) {
                            editor.insertBefore(afterPara, codeContainer.nextSibling);
                        } else {
                            editor.appendChild(afterPara);
                        }

                        range.setStart(afterPara, 0);
                        range.collapse(true);
                    }
                } else {
                    // Estamos diretamente no editor, insere normalmente
                    const codeContainer = createCodeBlock(language, code);
                    range.insertNode(codeContainer);

                    const afterPara = document.createElement('p');
                    afterPara.innerHTML = '<br>';
                    if (codeContainer.nextSibling) {
                        editor.insertBefore(afterPara, codeContainer.nextSibling);
                    } else {
                        editor.appendChild(afterPara);
                    }

                    range.setStart(afterPara, 0);
                    range.collapse(true);
                }
            } else {
                // Sem seleção, adiciona no final
                const codeContainer = createCodeBlock(language, code);
                editor.appendChild(codeContainer);

                const afterPara = document.createElement('p');
                afterPara.innerHTML = '<br>';
                editor.appendChild(afterPara);

                range = document.createRange();
                range.setStart(afterPara, 0);
                range.collapse(true);
            }

            selection.removeAllRanges();
            selection.addRange(range);

            closeCodeModal();
            save();
        }

        // Function to save content
        function save() {
            if (hiddenField) {
                hiddenField.value = editor.innerHTML;
            }
        }

        // Function to close editor
        function closeEditor() {
            if (confirm('Deseja realmente limpar? Alterações não salvas serão perdidas.')) {
                editor.innerHTML = '';
                save();
            }
        }

        // Bind toolbar buttons
        const cmdButtons = root.querySelectorAll('[data-cmd]');
        console.log(`Found ${cmdButtons.length} toolbar buttons with data-cmd`);
        cmdButtons.forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const cmd = btn.dataset.cmd;
                console.log('Button clicked:', cmd);
                if (cmd === 'link') {
                    insertLink();
                } else if (cmd === 'code') {
                    openCodeModal();
                } else if (cmd === 'formatBlock' && btn.dataset.format) {
                    formatText('formatBlock', btn.dataset.format);
                } else {
                    formatText(cmd);
                }
            });
        });

        // Bind format block select
        const formatBlockSelects = root.querySelectorAll('[data-format-block]');
        console.log(`Found ${formatBlockSelects.length} format-block selects`);
        formatBlockSelects.forEach(sel => {
            sel.addEventListener('change', () => {
                const v = sel.value;
                console.log('Format block changed:', v);
                if (v) {
                    formatText('formatBlock', v);
                    sel.selectedIndex = 0;
                }
            });
        });

        // Bind font name select
        const fontNameSelects = root.querySelectorAll('[data-font-name]');
        console.log(`Found ${fontNameSelects.length} font-name selects`);
        fontNameSelects.forEach(sel => {
            sel.addEventListener('change', () => {
                console.log('Font name changed:', sel.value);
                formatText('fontName', sel.value);
            });
        });

        // Bind font size select
        const fontSizeSelects = root.querySelectorAll('[data-font-size]');
        console.log(`Found ${fontSizeSelects.length} font-size selects`);
        fontSizeSelects.forEach(sel => {
            sel.addEventListener('change', () => {
                console.log('Font size changed:', sel.value);
                formatText('fontSize', sel.value);
            });
        });

        // Bind close button
        if (closeBtn) {
            closeBtn.addEventListener('click', closeEditor);
        }

        // Bind save button
        if (saveBtn) {
            saveBtn.addEventListener('click', () => {
                save();
                alert('Conteúdo sincronizado.');
            });
        }

        // Bind cancel button
        if (cancelBtn) {
            cancelBtn.addEventListener('click', closeEditor);
        }

        // Bind modal buttons
        if (modal) {
            const modalCancelBtn = modal.querySelector('[data-action="cancelCode"]');
            const modalInsertBtn = modal.querySelector('[data-action="insertCodeBlock"]');

            if (modalCancelBtn) {
                modalCancelBtn.addEventListener('click', closeCodeModal);
            }
            if (modalInsertBtn) {
                modalInsertBtn.addEventListener('click', insertCodeBlock);
            }
        }

        // Prevent editing code blocks
        editor.addEventListener('mousedown', (e) => {
            if (e.target.closest('.code-block-container')) {
                e.preventDefault();
            }
        });

        // Auto-save on input
        editor.addEventListener('input', save);
        
        // Also save on blur (when user clicks outside)
        editor.addEventListener('blur', save);
        
        // Save on keyup for immediate sync
        editor.addEventListener('keyup', save);

        // Save on form submit - use capturing phase to run first
        const form = root.closest('form');
        if (form) {
            form.addEventListener('submit', function(e) {
                // Sync content before form submission
                save();
                console.log('Rich Editor: synced on form submit, value:', hiddenField?.value?.substring(0, 100));
            }, true); // Use capturing phase
        }

        // Initial save
        save();

        console.log('Rich Editor initialized:', rootId, 'hidden field:', hiddenField?.id);
    };
})();

