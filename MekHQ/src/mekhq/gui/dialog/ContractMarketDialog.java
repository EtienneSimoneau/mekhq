/*
 * ContractMarketDialog.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import megamek.client.ui.preferences.*;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.market.ContractMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.universe.Factions;
import mekhq.gui.FactionComboBox;
import mekhq.gui.view.ContractSummaryPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

/**
 * Presents contract offers generated by ContractMarket
 *
 * Code borrowed heavily from PersonnelMarketDialog
 *
 * @author Neoancient
 */
public class ContractMarketDialog extends JDialog {
    /* Save these settings between instantiations */
    private static boolean payMRBC = true;
    private static int advance = 25;
    private static int signingBonus = 0;
    private static int sharePct = 20;

    private Campaign campaign;
    private ContractMarket contractMarket;
    private Contract selectedContract = null;
    private List<String> possibleRetainerContracts;

    private JScrollPane scrollContractView;
    private ContractSummaryPanel contractView;

    private JCheckBox chkMRBC;
    private JSpinner spnSigningBonus;
    private JSpinner spnAdvance;
    private JSpinner spnSharePct;
    private JTable tableContracts;
    private JLabel lblCurrentRetainer;
    private JLabel lblRetainerEmployer;
    private JButton btnEndRetainer;
    private JLabel lblRetainerAvailable;
    private FactionComboBox cbRetainerEmployer;
    private JButton btnStartRetainer;

    public ContractMarketDialog(Frame frame, Campaign c) {
        super(frame, true);
        campaign = c;
        contractMarket = c.getContractMarket();
        possibleRetainerContracts = new ArrayList<>();
        if (c.getFactionCode().equals("MERC")) {
            countSuccessfulContracts();
        }
        initComponents();
        setLocationRelativeTo(frame);
        setUserPreferences();
    }

    /* A balance of six or more successful contracts with the same
     * employer results in the offer of a retainer contract.
     */
    private void countSuccessfulContracts() {
        HashMap<String, Integer> successfulContracts = new HashMap<>();
        for (AtBContract contract : campaign.getCompletedAtBContracts()) {
            if (contract.getEmployerCode().equals(campaign.getRetainerEmployerCode())) {
                continue;
            }
            int num;
            num = successfulContracts.getOrDefault(contract.getEmployerCode(), 0);
            successfulContracts.put(contract.getEmployerCode(),
                    num + (contract.getStatus().isSuccess() ? 1 : -1));
        }
        for (String key : successfulContracts.keySet()) {
            if (successfulContracts.get(key) >= 6) {
                possibleRetainerContracts.add(key);
            }
        }
    }

    private void initComponents() {
        JScrollPane scrollTableContracts = new JScrollPane();
        scrollContractView = new JScrollPane();
        JPanel panelTable = new JPanel();
        JPanel panelFees = new JPanel();
        JPanel panelRetainer = new JPanel();
        JPanel panelOKBtns = new JPanel();
        contractView = null;
        JButton btnGenerate = new JButton();
        JButton btnRemove = new JButton();
        JButton btnAccept = new JButton();
        JButton btnClose = new JButton();

        chkMRBC = new JCheckBox();
        chkMRBC.addItemListener(arg0 -> {
            payMRBC = chkMRBC.isSelected();
            for (Contract c : contractMarket.getContracts()) {
                c.setMRBCFee(payMRBC);
                c.calculateContract(campaign);
            }
            if (contractView != null) {
                contractView.refreshAmounts();
            }
        });
        JLabel lblAdvance = new JLabel();
        spnAdvance = new JSpinner(new SpinnerNumberModel(advance, 0, 25, 5));
        spnAdvance.addChangeListener(arg0 -> {
            advance = (Integer) spnAdvance.getValue();
            for (Contract c : contractMarket.getContracts()) {
                c.setAdvancePct(advance);
                c.calculateContract(campaign);
            }
            if (contractView != null) {
                contractView.refreshAmounts();
            }
        });
        JLabel lblSigningBonus = new JLabel();
        spnSigningBonus = new JSpinner(new SpinnerNumberModel(signingBonus, 0, 10, 1));
        spnSigningBonus.addChangeListener(arg0 -> {
            signingBonus = (Integer) spnSigningBonus.getValue();
            for (Contract c : contractMarket.getContracts()) {
                c.setSigningBonusPct(signingBonus);
                c.calculateContract(campaign);
            }
            if (contractView != null) {
                contractView.refreshAmounts();
            }
        });

        JLabel lblSharePct = new JLabel();
        spnSharePct = new JSpinner(new SpinnerNumberModel(sharePct, 20, 50, 10));
        spnSharePct.addChangeListener(arg0 -> {
            sharePct = (Integer) spnSharePct.getValue();
            for (Contract c : contractMarket.getContracts()) {
                if (campaign.getCampaignOptions().getUseAtB()
                        && campaign.getCampaignOptions().getUseShareSystem()
                        && c instanceof AtBContract) {
                    ((AtBContract) c).setSharesPct(sharePct);
                    c.calculateContract(campaign);
                }
            }
            if (contractView != null) {
                contractView.refreshAmounts();
            }
        });

        lblCurrentRetainer = new JLabel();
        lblRetainerEmployer = new JLabel();
        btnEndRetainer = new JButton();
        lblRetainerAvailable = new JLabel();
        cbRetainerEmployer = new FactionComboBox();
        btnStartRetainer = new JButton();

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ContractMarketDialog",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("Form.title"));
        setName("Form");
        getContentPane().setLayout(new BorderLayout());

        scrollTableContracts.setMinimumSize(new Dimension(500, 400));
        scrollTableContracts.setName("scrollTableContracts");
        scrollTableContracts.setPreferredSize(new Dimension(500, 400));

        chkMRBC.setName("chkMRBC");
        chkMRBC.setText(resourceMap.getString("checkMRBC.text"));
        chkMRBC.setSelected(payMRBC);
        panelFees.add(chkMRBC);

        lblAdvance.setText(resourceMap.getString("lblAdvance.text"));
        panelFees.add(lblAdvance);
        panelFees.add(spnAdvance);
        lblSigningBonus.setText(resourceMap.getString("lblSigningBonus.text"));
        panelFees.add(lblSigningBonus);
        panelFees.add(spnSigningBonus);
        lblSharePct.setText(resourceMap.getString("lblSharePct.text"));
        if (campaign.getCampaignOptions().getUseShareSystem()) {
            panelFees.add(lblSharePct);
            panelFees.add(spnSharePct);
        }

        Vector<Vector<String>> data = new Vector<>();
        Vector<String> colNames = new Vector<>();
        for (Contract c : contractMarket.getContracts()) {
            // Changes in rating or force size since creation can alter some details
            if (c instanceof AtBContract) {
                final AtBContract atbContract = (AtBContract) c;
                atbContract.initContractDetails(campaign);
                atbContract.calculatePaymentMultiplier(campaign);
                atbContract.setPartsAvailabilityLevel(atbContract.getContractType().calculatePartsAvailabilityLevel());
                atbContract.setSharesPct(campaign.getCampaignOptions().getUseShareSystem()
                        ? (Integer) spnSharePct.getValue() : 0);
            }
            c.setStartDate(null);
            c.setMRBCFee(payMRBC);
            c.setAdvancePct(advance);
            c.setSigningBonusPct(signingBonus);

            c.calculateContract(campaign);
            Vector<String> row = new Vector<>();
            if (c instanceof AtBContract) {
                row.add(((AtBContract) c).getEmployerName(campaign.getGameYear()));
                row.add(((AtBContract) c).getEnemyName(campaign.getGameYear()));
                if (((AtBContract) c).isSubcontract()) {
                    row.add(((AtBContract) c).getContractType() + " (Subcontract)");
                } else {
                    row.add(((AtBContract) c).getContractType().toString());
                }
            } else {
                row.add(c.getEmployer());
                row.add("");
                row.add(c.getType());
            }
            data.add(row);
        }
        colNames.add("Employer");
        colNames.add("Enemy");
        colNames.add("Mission Type");

        tableContracts = new JTable();
        DefaultTableModel tblContractsModel = new DefaultTableModel(data, colNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableContracts.setModel(tblContractsModel);
        tableContracts.setName("tableContracts");
        tableContracts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableContracts.createDefaultColumnsFromModel();
        tableContracts.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tableContracts.getSelectionModel().addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                contractChanged();
            }
        });

        tableContracts.setIntercellSpacing(new Dimension(0, 0));
        tableContracts.setShowGrid(false);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tblContractsModel);
        tableContracts.setRowSorter(sorter);
        scrollTableContracts.setViewportView(tableContracts);

        scrollContractView.setMinimumSize(new Dimension(500, 600));
        scrollContractView.setPreferredSize(new Dimension(500, 600));
        scrollContractView.setViewportView(null);

        panelTable.setLayout(new BorderLayout());
        panelTable.add(panelFees, BorderLayout.PAGE_START);
        panelTable.add(scrollTableContracts, BorderLayout.CENTER);
        panelTable.add(panelRetainer, BorderLayout.PAGE_END);

        panelRetainer.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        lblCurrentRetainer.setText(resourceMap.getString("lblCurrentRetainer.text"));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        panelRetainer.add(lblCurrentRetainer, gbc);
        if (null != campaign.getRetainerEmployerCode()) {
            lblRetainerEmployer.setText(Factions.getInstance().getFaction(campaign.getRetainerEmployerCode()).getFullName(campaign.getGameYear()));
        }
        gbc.gridx = 1;
        gbc.gridy = 0;
        panelRetainer.add(lblRetainerEmployer, gbc);
        btnEndRetainer.setText(resourceMap.getString("btnEndRetainer.text"));
        gbc.gridx = 0;
        gbc.gridy = 1;
        panelRetainer.add(btnEndRetainer, gbc);
        lblCurrentRetainer.setVisible(null != campaign.getRetainerEmployerCode());
        lblRetainerEmployer.setVisible(null != campaign.getRetainerEmployerCode());
        btnEndRetainer.setVisible(null != campaign.getRetainerEmployerCode());
        btnEndRetainer.addActionListener(ev -> {
            campaign.setRetainerEmployerCode(null);
            lblCurrentRetainer.setVisible(false);
            lblRetainerEmployer.setVisible(false);
            btnEndRetainer.setVisible(false);
            //Add faction back to available ones
            countSuccessfulContracts();
            lblRetainerAvailable.setVisible(possibleRetainerContracts.size() > 0);
            cbRetainerEmployer.setVisible(possibleRetainerContracts.size() > 0);
            btnStartRetainer.setVisible(possibleRetainerContracts.size() > 0);
        });

        lblRetainerAvailable.setText(resourceMap.getString("lblRetainerAvailable.text"));
        gbc.gridx = 0;
        gbc.gridy = 2;
        panelRetainer.add(lblRetainerAvailable, gbc);
        cbRetainerEmployer.addFactionEntries(possibleRetainerContracts, campaign.getGameYear());
        gbc.gridx = 1;
        gbc.gridy = 2;
        panelRetainer.add(cbRetainerEmployer, gbc);
        btnStartRetainer.setText(resourceMap.getString("btnStartRetainer.text"));
        gbc.gridx = 0;
        gbc.gridy = 3;
        panelRetainer.add(btnStartRetainer, gbc);
        lblRetainerAvailable.setVisible(possibleRetainerContracts.size() > 0);
        cbRetainerEmployer.setVisible(possibleRetainerContracts.size() > 0);
        btnStartRetainer.setVisible(possibleRetainerContracts.size() > 0);
        btnStartRetainer.addActionListener(e -> {
            campaign.setRetainerEmployerCode(cbRetainerEmployer.getSelectedItemKey());
            lblCurrentRetainer.setVisible(true);
            lblRetainerEmployer.setVisible(true);
            btnEndRetainer.setVisible(true);
            lblRetainerEmployer.setText(Factions.getInstance().getFaction(campaign.getRetainerEmployerCode()).getFullName(campaign.getGameYear()));
            //Remove the selected faction and add the previous one, if any
            countSuccessfulContracts();
            lblRetainerAvailable.setVisible(possibleRetainerContracts.size() > 0);
            cbRetainerEmployer.setVisible(possibleRetainerContracts.size() > 0);
            btnStartRetainer.setVisible(possibleRetainerContracts.size() > 0);
        });

        JSplitPane splitMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelTable, scrollContractView);
        splitMain.setOneTouchExpandable(true);
        splitMain.setResizeWeight(0.0);
        getContentPane().add(splitMain, BorderLayout.CENTER);

        panelOKBtns.setLayout(new GridBagLayout());

        btnGenerate.setText(resourceMap.getString("btnGenerate.text"));
        btnGenerate.setName("btnGenerate");
        btnGenerate.addActionListener(evt -> {
            AtBContract c = contractMarket.addAtBContract(campaign);

            if (c == null) {
                campaign.addReport(resourceMap.getString("report.UnableToGMContract"));
                return;
            }

            c.initContractDetails(campaign);
            c.setPartsAvailabilityLevel(c.getContractType().calculatePartsAvailabilityLevel());
            c.setSharesPct(campaign.getCampaignOptions().getUseShareSystem()
                    ? (Integer) spnSharePct.getValue() : 0);
            c.setStartDate(null);
            c.setMRBCFee(payMRBC);
            c.setAdvancePct(advance);
            c.setSigningBonusPct(signingBonus);

            c.calculateContract(campaign);
            Vector<String> row = new Vector<>();
            row.add(c.getEmployerName(campaign.getGameYear()));
            row.add(c.getEnemyName(campaign.getGameYear()));
            row.add(c.getContractType().toString());

            ((DefaultTableModel) tableContracts.getModel()).addRow(row);
        });
        btnGenerate.setEnabled(campaign.isGM());
        panelOKBtns.add(btnGenerate, new GridBagConstraints());

        btnRemove.setText(resourceMap.getString("btnRemove.text"));
        btnRemove.setName("btnRemove");
        btnRemove.addActionListener(evt -> {
            contractMarket.removeContract(selectedContract);
            ((DefaultTableModel) tableContracts.getModel()).removeRow(tableContracts.convertRowIndexToModel(tableContracts.getSelectedRow()));
        });
        panelOKBtns.add(btnRemove, new GridBagConstraints());

        btnAccept.setText(resourceMap.getString("btnAccept.text"));
        btnAccept.setName("btnAccept");
        btnAccept.addActionListener(this::acceptContract);
        panelOKBtns.add(btnAccept, new GridBagConstraints());

        btnClose.setText(resourceMap.getString("btnClose.text"));
        btnClose.setName("btnClose");
        btnClose.addActionListener(this::btnCloseActionPerformed);
        panelOKBtns.add(btnClose, new GridBagConstraints());

        getContentPane().add(panelOKBtns, BorderLayout.PAGE_END);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ContractMarketDialog.class);

        chkMRBC.setName("payMRBCFee");
        preferences.manage(new JToggleButtonPreference(chkMRBC));

        spnAdvance.setName("advancePercentage");
        preferences.manage(new JIntNumberSpinnerPreference(spnAdvance));

        spnSigningBonus.setName("signingBonusPercentage");
        preferences.manage(new JIntNumberSpinnerPreference(spnSigningBonus));

        spnSharePct.setName("sharePercentage");
        preferences.manage(new JIntNumberSpinnerPreference(spnSharePct));

        tableContracts.setName("contractsTable");
        preferences.manage(new JTablePreference(tableContracts));

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    public Contract getContract() {
        return selectedContract;
    }

    private void acceptContract(ActionEvent evt) {
        if (selectedContract != null) {
            selectedContract.setName(contractView.getContractName());
            campaign.getFinances().credit(TransactionType.CONTRACT_PAYMENT, campaign.getLocalDate(),
                    selectedContract.getTotalAdvanceAmount(),
                    "Advance funds for " + selectedContract.getName());
            campaign.addMission(selectedContract);
            // must be invoked after campaign.addMission to ensure presence of mission ID
            selectedContract.acceptContract(campaign);
            contractMarket.removeContract(selectedContract);
            ((DefaultTableModel) tableContracts.getModel()).removeRow(tableContracts
                    .convertRowIndexToModel(tableContracts.getSelectedRow()));
            refreshContractView();
        }
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
        selectedContract = null;
        setVisible(false);
    }

    private void contractChanged() {
        int view = tableContracts.getSelectedRow();
        if (view < 0) {
            //selection got filtered away
            selectedContract = null;
            refreshContractView();
            return;
        }
        /* preserve the name given to the previous contract (if any) */
        if (selectedContract != null && contractView != null) {
            selectedContract.setName(contractView.getContractName());
        }

        selectedContract = contractMarket.getContracts().get(tableContracts.convertRowIndexToModel(view));
        refreshContractView();
    }

    void refreshContractView() {
        int row = tableContracts.getSelectedRow();
        if (row < 0) {
            contractView = null;
            scrollContractView.setViewportView(null);
            return;
        }
        contractView = new ContractSummaryPanel(selectedContract, campaign,
                campaign.getCampaignOptions().getUseAtB()
                        && selectedContract instanceof AtBContract
                        && !((AtBContract) selectedContract).isSubcontract());
        scrollContractView.setViewportView(contractView);
        //This odd code is to make sure that the scrollbar stays at the top
        //I cant just call it here, because it ends up getting reset somewhere later
        SwingUtilities.invokeLater(() -> scrollContractView.getVerticalScrollBar().setValue(0));
    }
}
